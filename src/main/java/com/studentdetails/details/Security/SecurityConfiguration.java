package com.studentdetails.details.Security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration with role-based access control.
 * Supports both client and organization deployments with flexible authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.security.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
    private String allowedOrigins;

    @Value("${app.security.enable-csrf:false}")
    private boolean enableCsrf;

    @Value("${app.security.permit-all-paths:/api/auth/**,/api/public/**}")
    private String permitAllPaths;

    /**
     * Configures the security filter chain.
     *
     * @param http the HttpSecurity object
     * @return the SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> {
                    if (!enableCsrf) {
                        csrf.disable();
                    }
                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Admin user management endpoints - require authentication (role checked via @PreAuthorize)
                        // These must come BEFORE the general /api/auth/** rule to take precedence
                        .requestMatchers(HttpMethod.POST, "/api/auth/admin/users").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/auth/role").authenticated()
                        // Public auth endpoints - no authentication required
                        .requestMatchers("/api/auth/google", "/api/auth/google/complete", "/api/auth/admin/login", "/api/auth/login").permitAll()
                        // Other auth endpoints - no authentication required
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        
                        // Admin endpoints - ADMIN role required
                        .requestMatchers(HttpMethod.GET, "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/**").hasRole("ADMIN")
                        
                        // Student endpoints - STUDENT, TEACHER, or ADMIN role required
                        // Support both /api/students and /students paths for backward compatibility
                        .requestMatchers("/api/students/**", "/students/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers("/api/marks/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        // Support both /api/courses and /courses paths for backward compatibility
                        .requestMatchers("/api/courses/**", "/courses/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .requestMatchers("/api/reports/**").hasAnyRole("TEACHER", "ADMIN")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures CORS settings.
     *
     * @return the CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configures the authentication provider.
     *
     * @return the AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Configures the password encoder.
     *
     * @return the PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the authentication manager.
     *
     * @param config the AuthenticationConfiguration
     * @return the AuthenticationManager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}


