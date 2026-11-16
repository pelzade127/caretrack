package com.caretrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.util.Arrays;

/**
 * Application-wide beans.
 *
 * <ul>
 *   <li>A {@link Clock} bean, so services never call {@code Instant.now()} directly and
 *       tests can supply a fixed clock.</li>
 *   <li>Development CORS, limited to {@code /api/**}, the listed origins, GET/POST and the
 *       Content-Type header. In normal local use the Vite dev server proxies {@code /api},
 *       so the browser never makes a cross-origin call; this only matters if the frontend
 *       is pointed straight at port 8080.</li>
 * </ul>
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public AppConfig(@Value("${caretrack.cors.allowed-origins:}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::strip)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type");
    }
}
