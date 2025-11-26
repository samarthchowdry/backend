package com.studentdetails.details.Service.ServiceImpl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.studentdetails.details.Domain.LoginInfo;
import com.studentdetails.details.Domain.UserRole;
import com.studentdetails.details.Repository.LoginInfoRepository;
import com.studentdetails.details.Service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {

    private final String clientId;
    private final LoginInfoRepository loginInfoRepository;

    public AuthServiceImpl(@Value("${google.clientId}") String clientId,
                           LoginInfoRepository loginInfoRepository) {
        this.clientId = clientId;
        this.loginInfoRepository = loginInfoRepository;
    }

    @Override
    @Transactional
    public ResponseEntity<?> verifyGoogleToken(Map<String, String> body) {
        String idTokenString = body.get("idToken");
        if (idTokenString == null || idTokenString.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing idToken");
        }
        if (clientId == null || clientId.isBlank() || clientId.contains("YOUR_GOOGLE_CLIENT_ID")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Google clientId not configured");
        }

        try {
            var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = JacksonFactory.getDefaultInstance();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            LoginInfo savedInfo = upsertLoginInfo(payload);

            Map<String, Object> result = new HashMap<>();
            result.put("email", savedInfo.getEmail());
            result.put("emailVerified", Boolean.TRUE.equals(payload.getEmailVerified()));
            result.put("name", savedInfo.getFullName());
            result.put("picture", savedInfo.getPictureUrl());
            result.put("lastLoginAt", savedInfo.getLastLoginAt());
            result.put("googleSub", savedInfo.getGoogleSub());
            result.put("role", savedInfo.getRole());

            return ResponseEntity.ok(result);

        } catch (GeneralSecurityException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Security error verifying token");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error verifying token");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> completeGoogleSignup(Map<String, String> body) {
        String googleSubRaw = body.get("googleSub");
        String emailRaw = body.get("email");
        String googleSub = googleSubRaw != null ? googleSubRaw.trim() : null;
        String email = emailRaw != null ? emailRaw.trim() : null;
        boolean hasGoogleSub = googleSub != null && !googleSub.isBlank();
        boolean hasEmail = email != null && !email.isBlank();

        if (!hasGoogleSub && !hasEmail) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("googleSub or email is required");
        }

        Optional<LoginInfo> loginInfoOptional = Optional.empty();
        if (hasGoogleSub) {
            loginInfoOptional = loginInfoRepository.findByGoogleSub(googleSub);
        }
        if (loginInfoOptional.isEmpty() && hasEmail) {
            loginInfoOptional = loginInfoRepository.findByEmail(email);
        }

        if (loginInfoOptional.isEmpty() && !hasEmail) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("email is required to create a new account");
        }

        String resolvedGoogleSub = hasGoogleSub ? googleSub : UUID.randomUUID().toString();

        LoginInfo loginInfo = loginInfoOptional.orElseGet(() -> LoginInfo.builder()
                .googleSub(resolvedGoogleSub)
                .email(email)
                .role(UserRole.STUDENT)
                .build());

        loginInfo.setGoogleSub(resolvedGoogleSub);
        if (hasEmail) {
            loginInfo.setEmail(email);
        }
        loginInfo.setFullName(body.get("name"));
        loginInfo.setPictureUrl(body.get("picture"));
        loginInfo.setLastLoginAt(LocalDateTime.now());
        if (loginInfo.getRole() == null) {
            loginInfo.setRole(UserRole.STUDENT);
        }
        if (loginInfo.getProjectAdmin() == null) {
            loginInfo.setProjectAdmin(Boolean.FALSE);
        }

        try {
            LoginInfo saved = loginInfoRepository.save(loginInfo);
            Map<String, Object> response = new HashMap<>();
            response.put("email", saved.getEmail());
            response.put("name", saved.getFullName());
            response.put("picture", saved.getPictureUrl());
            response.put("lastLoginAt", saved.getLastLoginAt());
            response.put("googleSub", saved.getGoogleSub());
            response.put("role", saved.getRole());
            return ResponseEntity.ok(response);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already in use by another account");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateUserRole(String roleHeader, Map<String, String> body) {
        UserRole requesterRole = resolveRole(roleHeader);
        enforceAdmin(requesterRole);

        String roleValue = body.get("role");
        String email = body.get("email");
        String googleSub = body.get("googleSub");

        if ((email == null || email.isBlank()) && (googleSub == null || googleSub.isBlank())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "email or googleSub is required"));
        }
        if (roleValue == null || roleValue.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "role is required"));
        }

        UserRole newRole;
        try {
            newRole = UserRole.valueOf(roleValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid role value"));
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
                    .body(Map.of("message", "User not found"));
        }

        LoginInfo loginInfo = loginInfoOptional.get();
        loginInfo.setRole(newRole);
        LoginInfo saved = loginInfoRepository.save(loginInfo);

        Map<String, Object> response = new HashMap<>();
        response.put("email", saved.getEmail());
        response.put("role", saved.getRole());
        response.put("googleSub", saved.getGoogleSub());
        response.put("name", saved.getFullName());

        return ResponseEntity.ok(response);
    }

    private LoginInfo upsertLoginInfo(GoogleIdToken.Payload payload) {
        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
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
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid role value");
        }
    }

    private void enforceAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin privileges required");
        }
    }
}

