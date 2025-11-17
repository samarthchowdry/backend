package com.studentdetails.details.Service;

import java.util.Map;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> verifyGoogleToken(Map<String, String> body);
    ResponseEntity<?> completeGoogleSignup(Map<String, String> body);
    ResponseEntity<?> updateUserRole(String roleHeader, Map<String, String> body);
}
