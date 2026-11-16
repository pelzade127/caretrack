package com.caretrack.seed;

import com.caretrack.dto.HistoryEventResponse;
import com.caretrack.entity.EventType;
import com.caretrack.repository.SupportRequestRepository;
import com.caretrack.service.SupportRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seeder is switched off in the test profile, so this test builds one by hand and
 * runs it against the in-memory database.
 */
@SpringBootTest
@ActiveProfiles("test")
class DemoDataSeederTest {

    @Autowired
    private SupportRequestRepository repository;

    @Autowired
    private SupportRequestService service;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void seedingTwiceDoesNotCreateDuplicates() {
        DemoDataSeeder seeder = new DemoDataSeeder(repository, Clock.systemUTC());

        assertEquals(DemoDataSeeder.seedCount(), seeder.seed());
        assertEquals(DemoDataSeeder.seedCount(), repository.count());

        assertEquals(0, seeder.seed());
        assertEquals(DemoDataSeeder.seedCount(), repository.count());
    }

    @Test
    void seedDataIncludesDemoCustomerRequestsAndARecordedReopening() {
        new DemoDataSeeder(repository, Clock.systemUTC()).seed();

        assertEquals(3, service.listForDemoCustomer().size());

        var reopened = service.getForAgent("CT-100005");
        assertTrue(reopened.history().stream()
                .map(HistoryEventResponse::type)
                .anyMatch(type -> type == EventType.REOPENED));
    }
}
