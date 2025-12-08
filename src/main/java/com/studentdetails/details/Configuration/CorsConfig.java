package com.studentdetails.details.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuration class for CORS (Cross-Origin Resource Sharing) settings.
 * Note: CORS is now primarily configured in SecurityConfiguration.
 * This class is kept for backward compatibility but may be deprecated.
 * This class is used by Spring Framework for configuration via component scanning.
 */
@Configuration
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class CorsConfig {
    /**
     * Creates a CORS filter bean for handling cross-origin requests.
     * Note: CORS is now primarily handled by SecurityConfiguration.
     * This method is called by Spring Framework to create the bean.
     *
     * @return the CORS filter
     */
    @Bean
    @SuppressWarnings("unused") // Suppress unused warning - method is used by Spring Framework
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}


