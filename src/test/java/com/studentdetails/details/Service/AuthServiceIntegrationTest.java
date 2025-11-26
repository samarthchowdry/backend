package com.studentdetails.details.Service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Test
    void completeSignup_createsUserInRealDatabase() {
        Map<String, String> body = new HashMap<>();
        body.put("googleSub", "integration-test-sub");
        body.put("email", "integration@test.local");
        body.put("name", "Integration Test");

        var response = authService.completeGoogleSignup(body);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}

