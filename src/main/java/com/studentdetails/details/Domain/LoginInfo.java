package com.studentdetails.details.Domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "login_info")
public class LoginInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_sub", unique = true, nullable = false, length = 64)
    private String googleSub;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "picture_url", length = 1024)
    private String pictureUrl;

    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Column(name = "is_project_admin", nullable = false)
    private Boolean projectAdmin;

    @Column(name = "password")
    private String password;

    @PrePersist
    @PreUpdate
    private void ensureDefaults() {
        if (googleSub == null || googleSub.isBlank()) {
            googleSub = UUID.randomUUID().toString();
        }
        if (role == null) {
            role = UserRole.STUDENT;
        }
        if (lastLoginAt == null) {
            lastLoginAt = LocalDateTime.now();
        }
        if (projectAdmin == null) {
            projectAdmin = Boolean.FALSE;
        }
    }
}
