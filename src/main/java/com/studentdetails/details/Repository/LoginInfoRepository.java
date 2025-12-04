package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.LoginInfo;
import com.studentdetails.details.Domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoginInfoRepository extends JpaRepository<LoginInfo, Long> {
    Optional<LoginInfo> findByGoogleSub(String googleSub);
    Optional<LoginInfo> findByEmail(String email);
    List<LoginInfo> findByRole(UserRole role);
    Optional<LoginInfo> findFirstByRoleOrderByLastLoginAtDesc(UserRole role);
}

