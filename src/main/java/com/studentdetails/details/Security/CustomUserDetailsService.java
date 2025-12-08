package com.studentdetails.details.Security;

import com.studentdetails.details.Domain.LoginInfo;
import com.studentdetails.details.Domain.UserRole;
import com.studentdetails.details.Repository.LoginInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user details from the database for authentication.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class CustomUserDetailsService implements UserDetailsService {

    private final LoginInfoRepository loginInfoRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        LoginInfo loginInfo = loginInfoRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        Collection<? extends GrantedAuthority> authorities = getAuthorities(loginInfo.getRole());

        return User.builder()
                .username(loginInfo.getEmail())
                .password(loginInfo.getPassword() != null ? loginInfo.getPassword() : "{noop}")
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * Converts UserRole to Spring Security authorities.
     *
     * @param role the user role
     * @return collection of granted authorities
     */
    private Collection<? extends GrantedAuthority> getAuthorities(UserRole role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}






