package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.LoginInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginInfoRepository extends JpaRepository<LoginInfo, Long> {
    Optional<LoginInfo> findByGoogleSub(String googleSub);
    Optional<LoginInfo> findByEmail(String email);
}

