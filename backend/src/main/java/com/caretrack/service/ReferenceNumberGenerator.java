package com.caretrack.service;

import com.caretrack.repository.SupportRequestRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates public reference numbers like {@code CT-7K3QXM}.
 *
 * The alphabet leaves out look-alike characters (0/O, 1/I/L) so a customer can read a
 * reference over the phone. Seed data uses references such as {@code CT-100001}; because
 * they contain 0 and 1, a generated reference can never collide with a seed reference.
 * The database's unique constraint on the column is the final safeguard.
 */
@Component
public class ReferenceNumberGenerator {

    static final String PREFIX = "CT-";
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 6;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final SupportRequestRepository repository;

    public ReferenceNumberGenerator(SupportRequestRepository repository) {
        this.repository = repository;
    }

    public String nextUnique() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = randomReference();
            if (!repository.existsByReferenceNumber(candidate)) {
                return candidate;
            }
        }
        // With ~887 million combinations this is practically unreachable for a demo.
        throw new IllegalStateException("Could not generate a unique reference number");
    }

    String randomReference() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
