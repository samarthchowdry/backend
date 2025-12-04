package com.studentdetails.details.Resources;
import com.studentdetails.details.Service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<?> verifyGoogleToken(@RequestBody Map<String, String> body) {
        return authService.verifyGoogleToken(body);
    }

    @PostMapping("/google/complete")
    public ResponseEntity<?> completeGoogleSignup(@RequestBody Map<String, String> body) {
        return authService.completeGoogleSignup(body);
    }

    @PatchMapping("/role")
    public ResponseEntity<?> updateUserRole(
            @RequestHeader(name = "X-Role", required = false) String roleHeader,
            @RequestBody Map<String, String> body) {
        return authService.updateUserRole(roleHeader, body);
    }
}
