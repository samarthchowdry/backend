package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);
    
    Optional<Admin> findByGoogleSub(String googleSub);
    
    boolean existsByEmail(String email);
    
    boolean existsByGoogleSub(String googleSub);
}

