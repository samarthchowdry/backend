package com.studentdetails.details.Service.ServiceImpl;

import com.studentdetails.details.Domain.LoginInfo;
import com.studentdetails.details.Domain.UserRole;
import com.studentdetails.details.Repository.LoginInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private LoginInfoRepository loginInfoRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl("test-client-id", loginInfoRepository);
    }

    @Test
    void completeGoogleSignup_createsLoginInfo_whenNotFound() {
        Map<String, String> body = new HashMap<>();
        body.put("googleSub", "local-sub");
        body.put("email", "new-user@example.com");
        body.put("name", "New User");
        body.put("picture", "http://example.com/avatar.png");

        when(loginInfoRepository.findByGoogleSub("local-sub")).thenReturn(Optional.empty());
        when(loginInfoRepository.findByEmail("new-user@example.com")).thenReturn(Optional.empty());

        LoginInfo saved = LoginInfo.builder()
                .id(1L)
                .googleSub("local-sub")
                .email("new-user@example.com")
                .fullName("New User")
                .pictureUrl("http://example.com/avatar.png")
                .lastLoginAt(LocalDateTime.now())
                .role(UserRole.STUDENT)
                .build();
        when(loginInfoRepository.save(any(LoginInfo.class))).thenReturn(saved);

        ResponseEntity<?> response = authService.completeGoogleSignup(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody).containsEntry("email", "new-user@example.com");
        assertThat(responseBody).containsEntry("googleSub", "local-sub");

        ArgumentCaptor<LoginInfo> captor = ArgumentCaptor.forClass(LoginInfo.class);
        verify(loginInfoRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new-user@example.com");
        assertThat(captor.getValue().getGoogleSub()).isEqualTo("local-sub");
    }

    @Test
    void completeGoogleSignup_generatesGoogleSub_whenMissing() {
        Map<String, String> body = new HashMap<>();
        body.put("email", "local-only@example.com");
        body.put("name", "Local Only");

        when(loginInfoRepository.findByEmail("local-only@example.com")).thenReturn(Optional.empty());

        LoginInfo saved = LoginInfo.builder()
                .id(2L)
                .googleSub("generated-sub")
                .email("local-only@example.com")
                .fullName("Local Only")
                .lastLoginAt(LocalDateTime.now())
                .role(UserRole.STUDENT)
                .build();
        when(loginInfoRepository.save(any(LoginInfo.class))).thenReturn(saved);

        ResponseEntity<?> response = authService.completeGoogleSignup(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody).containsEntry("email", "local-only@example.com");
        assertThat(responseBody.get("googleSub")).isNotNull();

        verify(loginInfoRepository).save(any(LoginInfo.class));
    }

    @Test
    void completeGoogleSignup_requiresEmailWhenNewAccount() {
        Map<String, String> body = new HashMap<>();
        body.put("googleSub", "sub-only");

        when(loginInfoRepository.findByGoogleSub("sub-only")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authService.completeGoogleSignup(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("email is required to create a new account");
    }

    @Test
    void completeGoogleSignup_updatesExistingWithoutEmail() {
        Map<String, String> body = new HashMap<>();
        body.put("googleSub", "existing-sub");

        LoginInfo existing = LoginInfo.builder()
                .id(10L)
                .googleSub("existing-sub")
                .email("stored@example.com")
                .role(UserRole.ADMIN)
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .build();

        when(loginInfoRepository.findByGoogleSub("existing-sub")).thenReturn(Optional.of(existing));
        when(loginInfoRepository.save(any(LoginInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = authService.completeGoogleSignup(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertThat(responseBody).containsEntry("email", "stored@example.com");
        verify(loginInfoRepository).save(any(LoginInfo.class));
    }
}

