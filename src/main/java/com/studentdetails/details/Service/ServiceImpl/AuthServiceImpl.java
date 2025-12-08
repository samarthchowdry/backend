package com.studentdetails.details.Service.ServiceImpl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.studentdetails.details.Domain.*;
import com.studentdetails.details.Repository.*;
import com.studentdetails.details.Security.JwtTokenProvider;
import com.studentdetails.details.Service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String KEY_EMAIL = "email";
    private static final String KEY_PICTURE = "picture";
    private static final String KEY_GOOGLE_SUB = "googleSub";
    private static final String KEY_MESSAGE = "message";

    private final String clientId;
    private final LoginInfoRepository loginInfoRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AdminRepository adminRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(@Value("${google.clientId}") String clientId,
                           LoginInfoRepository loginInfoRepository,
                           StudentRepository studentRepository,
                           TeacherRepository teacherRepository,
                           AdminRepository adminRepository,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordEncoder passwordEncoder) {
        this.clientId = clientId;
        this.loginInfoRepository = loginInfoRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.adminRepository = adminRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public ResponseEntity<Object> verifyGoogleToken(Map<String, String> body) {
        String idTokenString = body.get("idToken");
        if (idTokenString == null || idTokenString.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing idToken");
        }
        if (clientId == null || clientId.isBlank() || clientId.contains("YOUR_GOOGLE_CLIENT_ID")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Google clientId not configured");
        }

        try {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            LoginInfo savedInfo = upsertLoginInfo(payload);
            
            // Save user to appropriate role-specific table
            saveUserToRoleSpecificTable(savedInfo);
            
            // Generate JWT token
            String token = jwtTokenProvider.generateToken(
                    savedInfo.getEmail(),
                    savedInfo.getRole(),
                    savedInfo.getId()
            );

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put(KEY_EMAIL, savedInfo.getEmail());
            result.put("emailVerified", Boolean.TRUE.equals(payload.getEmailVerified()));
            result.put("name", savedInfo.getFullName());
            result.put(KEY_PICTURE, savedInfo.getPictureUrl());
            result.put("lastLoginAt", savedInfo.getLastLoginAt());
            result.put(KEY_GOOGLE_SUB, savedInfo.getGoogleSub());
            result.put("role", savedInfo.getRole());
            result.put("userId", savedInfo.getId());

            return ResponseEntity.ok(result);

        } catch (GeneralSecurityException _) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Security error verifying token");
        } catch (Exception _) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error verifying token");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Object> completeGoogleSignup(Map<String, String> body) {
        String googleSubRaw = body.get(KEY_GOOGLE_SUB);
        String emailRaw = body.get(KEY_EMAIL);
        String googleSub = googleSubRaw != null ? googleSubRaw.trim() : null;
        String email = emailRaw != null ? emailRaw.trim() : null;
        boolean hasGoogleSub = googleSub != null && !googleSub.isBlank();
        boolean hasEmail = email != null && !email.isBlank();

        if (!hasGoogleSub && !hasEmail) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("googleSub or email is required");
        }

        Optional<LoginInfo> loginInfoOptional = findLoginInfo(hasGoogleSub, googleSub, hasEmail, email);

        if (loginInfoOptional.isEmpty() && !hasEmail) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("email is required to create a new account");
        }

        String resolvedGoogleSub = hasGoogleSub ? googleSub : UUID.randomUUID().toString();
        LoginInfo existingLoginInfo = loginInfoOptional.orElse(null);
        LoginInfo loginInfo = createOrUpdateLoginInfo(existingLoginInfo, resolvedGoogleSub, email, body);

        try {
            LoginInfo saved = loginInfoRepository.save(loginInfo);
            return buildSuccessResponse(saved);
        } catch (DataIntegrityViolationException _) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use by another account");
        }
    }

    private Optional<LoginInfo> findLoginInfo(boolean hasGoogleSub, String googleSub, boolean hasEmail, String email) {
        if (hasGoogleSub) {
            Optional<LoginInfo> found = loginInfoRepository.findByGoogleSub(googleSub);
            if (found.isPresent()) {
                return found;
            }
        }
        if (hasEmail) {
            return loginInfoRepository.findByEmail(email);
        }
        return Optional.empty();
    }

    private LoginInfo createOrUpdateLoginInfo(LoginInfo existingLoginInfo, String resolvedGoogleSub,
                                             String email, Map<String, String> body) {
        LoginInfo loginInfo = existingLoginInfo != null
                ? existingLoginInfo
                : LoginInfo.builder()
                        .googleSub(resolvedGoogleSub)
                        .email(email)
                        .role(UserRole.STUDENT)
                        .build();

        loginInfo.setGoogleSub(resolvedGoogleSub);
        if (email != null && !email.isBlank()) {
            loginInfo.setEmail(email);
        }
        loginInfo.setFullName(body.get("name"));
        loginInfo.setPictureUrl(body.get(KEY_PICTURE));
        loginInfo.setLastLoginAt(LocalDateTime.now());
        if (loginInfo.getRole() == null) {
            loginInfo.setRole(UserRole.STUDENT);
        }
        if (loginInfo.getProjectAdmin() == null) {
            loginInfo.setProjectAdmin(Boolean.FALSE);
        }
        return loginInfo;
    }

    private ResponseEntity<Object> buildSuccessResponse(LoginInfo saved) {
        // Save user to appropriate role-specific table
        saveUserToRoleSpecificTable(saved);
        
        // Generate JWT token
        String token = jwtTokenProvider.generateToken(
                saved.getEmail(),
                saved.getRole(),
                saved.getId()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put(KEY_EMAIL, saved.getEmail());
        response.put("name", saved.getFullName());
        response.put(KEY_PICTURE, saved.getPictureUrl());
        response.put("lastLoginAt", saved.getLastLoginAt());
        response.put(KEY_GOOGLE_SUB, saved.getGoogleSub());
        response.put("role", saved.getRole());
        response.put("userId", saved.getId());
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> updateUserRole(String roleHeader, String authorizationHeader, Map<String, String> body) {
        // Try to get role from JWT token first (more secure), fallback to X-Role header
        UserRole requesterRole = null;
        
        // Extract role from JWT token if Authorization header is provided
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            try {
                String token = authorizationHeader.startsWith("Bearer ") 
                    ? authorizationHeader.substring(7) 
                    : authorizationHeader;
                if (jwtTokenProvider.validateToken(token)) {
                    requesterRole = jwtTokenProvider.getRoleFromToken(token);
                }
            } catch (Exception e) {
                // If JWT extraction fails, fall back to header
            }
        }
        
        // Fallback to X-Role header if JWT extraction didn't work
        if (requesterRole == null) {
            requesterRole = resolveRole(roleHeader);
        }
        
        enforceAdmin(requesterRole);

        String roleValue = body.get("role");
        String email = body.get(KEY_EMAIL);
        String googleSub = body.get(KEY_GOOGLE_SUB);

        if ((email == null || email.isBlank()) && (googleSub == null || googleSub.isBlank())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_MESSAGE, "email or googleSub is required"));
        }
        if (roleValue == null || roleValue.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_MESSAGE, "role is required"));
        }

        UserRole newRole;
        try {
            newRole = UserRole.valueOf(roleValue.trim().toUpperCase());
        } catch (IllegalArgumentException _) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_MESSAGE, "Invalid role value"));
        }

        Optional<LoginInfo> loginInfoOptional = Optional.empty();
        if (googleSub != null && !googleSub.isBlank()) {
            loginInfoOptional = loginInfoRepository.findByGoogleSub(googleSub);
        }
        if (loginInfoOptional.isEmpty() && email != null && !email.isBlank()) {
            loginInfoOptional = loginInfoRepository.findByEmail(email);
        }

        if (loginInfoOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(KEY_MESSAGE, "User not found"));
        }

        LoginInfo loginInfo = loginInfoOptional.get();
        UserRole oldRole = loginInfo.getRole();
        loginInfo.setRole(newRole);
        LoginInfo saved = loginInfoRepository.save(loginInfo);
        
        // If role changed, update role-specific tables
        if (oldRole != newRole) {
            // Remove from old role table (optional - you may want to keep history)
            // For now, we'll just save to the new role table
            saveUserToRoleSpecificTable(saved);
        } else {
            // Just update the existing role table
            saveUserToRoleSpecificTable(saved);
        }

        Map<String, Object> response = new HashMap<>();
        response.put(KEY_EMAIL, saved.getEmail());
        response.put("role", saved.getRole());
        response.put(KEY_GOOGLE_SUB, saved.getGoogleSub());
        response.put("name", saved.getFullName());

        return ResponseEntity.ok(response);
    }

    private LoginInfo upsertLoginInfo(GoogleIdToken.Payload payload) {
        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get(KEY_PICTURE);
        LocalDateTime now = LocalDateTime.now();

        LoginInfo loginInfo = loginInfoRepository.findByGoogleSub(googleSub)
                .orElseGet(() -> loginInfoRepository.findByEmail(email).orElse(null));

        if (loginInfo == null) {
            loginInfo = LoginInfo.builder()
                    .googleSub(googleSub)
                    .email(email)
                    .role(UserRole.STUDENT)
                    .build();
        }

        loginInfo.setGoogleSub(googleSub);
        loginInfo.setEmail(email);
        loginInfo.setFullName(name);
        loginInfo.setPictureUrl(picture);
        loginInfo.setLastLoginAt(now);

        if (loginInfo.getRole() == null) {
            loginInfo.setRole(UserRole.STUDENT);
        }

        return loginInfoRepository.save(loginInfo);
    }

    private UserRole resolveRole(String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing role information");
        }
        try {
            return UserRole.valueOf(roleHeader.trim().toUpperCase());
        } catch (IllegalArgumentException _) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid role value");
        }
    }

    private void enforceAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin privileges required");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<Object> login(Map<String, String> body) {
        // This is the unified login endpoint for all user types (Student, Teacher, Admin)
        // It uses Google Sign-In authentication
        return verifyGoogleToken(body);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> loginWithCredentials(Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_MESSAGE, "Username/email and password are required"));
        }

        // Find user by email (username can be email) - use case-insensitive lookup
        Optional<LoginInfo> loginInfoOptional = loginInfoRepository.findByEmailIgnoreCase(username.trim());

        if (loginInfoOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_MESSAGE, "Invalid credentials"));
        }

        LoginInfo loginInfo = loginInfoOptional.get();

        // Verify password
        String storedPassword = loginInfo.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            LOGGER.warn("Login attempt for {} failed: Password not set in LoginInfo", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_MESSAGE, "Password not set. Please use Google sign-in or contact administrator."));
        }

        // Check if password is BCrypt encoded or plain text (for backward compatibility)
        boolean passwordMatches = false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            // BCrypt encoded password
            passwordMatches = passwordEncoder.matches(password, storedPassword);
            LOGGER.debug("Password check for {}: BCrypt encoded, match={}", username, passwordMatches);
        } else {
            // Plain text password (for backward compatibility - not recommended)
            passwordMatches = storedPassword.equals(password);
            LOGGER.debug("Password check for {}: Plain text, match={}", username, passwordMatches);
        }

        if (!passwordMatches) {
            LOGGER.warn("Login attempt for {} failed: Password mismatch", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_MESSAGE, "Invalid credentials"));
        }
        
        LOGGER.info("Login successful for {} (role: {})", username, loginInfo.getRole());

        // Update last login time
        loginInfo.setLastLoginAt(LocalDateTime.now());
        LoginInfo saved = loginInfoRepository.save(loginInfo);

        // Save user to appropriate role-specific table
        saveUserToRoleSpecificTable(saved);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(
                saved.getEmail(),
                saved.getRole(),
                saved.getId()
        );

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put(KEY_EMAIL, saved.getEmail());
        response.put("name", saved.getFullName());
        response.put(KEY_PICTURE, saved.getPictureUrl());
        response.put("lastLoginAt", saved.getLastLoginAt());
        response.put(KEY_GOOGLE_SUB, saved.getGoogleSub());
        response.put("role", saved.getRole());
        response.put("userId", saved.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Saves or updates user in the appropriate role-specific table based on their role.
     * This ensures users are stored in separate tables (students, teachers, admins).
     */
    private void saveUserToRoleSpecificTable(LoginInfo loginInfo) {
        String googleSub = loginInfo.getGoogleSub();
        String email = loginInfo.getEmail();
        String name = loginInfo.getFullName();
        String picture = loginInfo.getPictureUrl();
        LocalDateTime now = LocalDateTime.now();
        UserRole role = loginInfo.getRole();

        switch (role) {
            case STUDENT:
                saveOrUpdateStudent(googleSub, email, name, picture, now);
                break;
            case TEACHER:
                saveOrUpdateTeacher(googleSub, email, name, picture, now);
                break;
            case ADMIN:
                saveOrUpdateAdmin(googleSub, email, name, picture, now, loginInfo.getProjectAdmin());
                break;
        }
    }

    private void saveOrUpdateStudent(String googleSub, String email, String name, String picture, LocalDateTime now) {
        // Find student by email (since Student entity doesn't have findByEmail in repository)
        Optional<Student> existingStudent = studentRepository.findAll().stream()
                .filter(s -> email != null && email.equalsIgnoreCase(s.getEmail()))
                .findFirst();
        
        if (existingStudent.isEmpty() && googleSub != null) {
            // Try to find by any identifier if available
            existingStudent = studentRepository.findAll().stream()
                    .filter(s -> s.getEmail() != null && s.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        }
        
        Student student;
        
        if (existingStudent.isPresent()) {
            student = existingStudent.get();
            if (name != null && !name.isBlank()) {
                student.setName(name);
            }
            // Note: Student entity has required fields (dob, address, branch) that we can't change here
            // We only update what we can
        } else {
            // For new students via Google login, we would need to create with default values
            // But since Student has required fields, we'll skip creation here
            // Students should be created through the proper student management flow
            // We'll just update existing students if found
            return; // Don't create new students automatically
        }
        
        studentRepository.save(student);
    }

    private void saveOrUpdateTeacher(String googleSub, String email, String name, String picture, LocalDateTime now) {
        Optional<Teacher> existingTeacher = teacherRepository.findByEmail(email);
        
        if (existingTeacher.isEmpty() && googleSub != null) {
            existingTeacher = teacherRepository.findByGoogleSub(googleSub);
        }
        
        Teacher teacher;
        
        if (existingTeacher.isPresent()) {
            teacher = existingTeacher.get();
            teacher.setName(name != null ? name : teacher.getName());
            teacher.setEmail(email);
            teacher.setGoogleSub(googleSub);
            teacher.setPictureUrl(picture);
            teacher.setLastLoginAt(now);
        } else {
            teacher = new Teacher();
            teacher.setName(name != null ? name : "Teacher");
            teacher.setEmail(email);
            teacher.setGoogleSub(googleSub);
            teacher.setPictureUrl(picture);
            teacher.setLastLoginAt(now);
        }
        
        teacherRepository.save(teacher);
    }

    private void saveOrUpdateAdmin(String googleSub, String email, String name, String picture, 
                                   LocalDateTime now, Boolean projectAdmin) {
        Optional<Admin> existingAdmin = adminRepository.findByEmail(email);
        
        if (existingAdmin.isEmpty() && googleSub != null) {
            existingAdmin = adminRepository.findByGoogleSub(googleSub);
        }
        
        Admin admin;
        
        if (existingAdmin.isPresent()) {
            admin = existingAdmin.get();
            admin.setName(name != null ? name : admin.getName());
            admin.setEmail(email);
            admin.setGoogleSub(googleSub);
            admin.setPictureUrl(picture);
            admin.setLastLoginAt(now);
            if (projectAdmin != null) {
                admin.setProjectAdmin(projectAdmin);
            }
        } else {
            admin = new Admin();
            admin.setName(name != null ? name : "Admin");
            admin.setEmail(email);
            admin.setGoogleSub(googleSub);
            admin.setPictureUrl(picture);
            admin.setLastLoginAt(now);
            admin.setProjectAdmin(projectAdmin != null ? projectAdmin : Boolean.FALSE);
        }
        
        adminRepository.save(admin);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> createUser(Map<String, String> body) {
        String email = body.get(KEY_EMAIL);
        String password = body.get("password");
        String roleValue = body.get("role");
        String fullName = body.get("fullName");

        // Validate required fields
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_MESSAGE, "Email is required"));
        }
        if (password == null || password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_MESSAGE, "Password is required"));
        }
        if (password.length() < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_MESSAGE, "Password must be at least 6 characters long"));
        }
        if (roleValue == null || roleValue.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_MESSAGE, "Role is required"));
        }

        // Validate role
        UserRole role;
        try {
            role = UserRole.valueOf(roleValue.trim().toUpperCase());
        } catch (IllegalArgumentException _) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_MESSAGE, "Invalid role. Must be ADMIN or TEACHER"));
        }

        // Only allow creating ADMIN or TEACHER (not STUDENT via this endpoint)
        if (role == UserRole.STUDENT) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(KEY_MESSAGE, "Cannot create STUDENT users via this endpoint. Students should sign up via Google or login page."));
        }

        // Check if user already exists
        Optional<LoginInfo> existingUser = loginInfoRepository.findByEmail(email.trim().toLowerCase());
        if (existingUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(KEY_MESSAGE, "User with this email already exists"));
        }

        // Generate a unique googleSub (required field)
        String googleSub = "manual-" + UUID.randomUUID().toString();

        // Hash password using BCrypt
        String hashedPassword = passwordEncoder.encode(password);

        // Create new user
        LoginInfo newUser = LoginInfo.builder()
                .googleSub(googleSub)
                .email(email.trim().toLowerCase())
                .fullName(fullName != null && !fullName.isBlank() ? fullName.trim() : null)
                .role(role)
                .password(hashedPassword)
                .lastLoginAt(LocalDateTime.now())
                .projectAdmin(role == UserRole.ADMIN)
                .build();

        try {
            LoginInfo saved = loginInfoRepository.save(newUser);
            
            // Save user to appropriate role-specific table
            saveUserToRoleSpecificTable(saved);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put(KEY_EMAIL, saved.getEmail());
            response.put("fullName", saved.getFullName());
            response.put("role", saved.getRole());
            response.put("userId", saved.getId());
            response.put(KEY_MESSAGE, "User created successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DataIntegrityViolationException _) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(KEY_MESSAGE, "User with this email or Google Sub already exists"));
        }
    }
}

