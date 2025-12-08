package com.studentdetails.details;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class DetailsApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command line arguments
     */
    static void main(String[] args) {
        SpringApplication.run(DetailsApplication.class, args);
    }

}
