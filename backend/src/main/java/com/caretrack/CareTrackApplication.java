package com.caretrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the CareTrack backend.
 *
 * {@code @SpringBootApplication} turns on component scanning for this package and its
 * sub-packages, plus Spring Boot auto-configuration (web server, JPA, validation, ...).
 */
@SpringBootApplication
public class CareTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareTrackApplication.class, args);
    }
}
