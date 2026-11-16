package com.caretrack.controller;

import com.caretrack.entity.RequestCategory;
import com.caretrack.entity.RequestEvent;
import com.caretrack.entity.SupportRequest;
import com.caretrack.repository.SupportRequestRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the real HTTP layer (MockMvc), service, JPA and an in-memory
 * H2 database. Each test starts from an empty database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestApiIntegrationTest {

    private static final String CUSTOMER = "/api/customer/requests";
    private static final String AGENT = "/api/agent/requests";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SupportRequestRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    // ------------------------------------------------------------------ creation

    @Test
    void createReturns201WithReferenceLocationAndInitialHistory() throws Exception {
        mvc.perform(post(CUSTOMER).contentType(APPLICATION_JSON).content("""
                        {
                          "customerName": "  Jordan Rivera ",
                          "category": "SERVICE_ISSUE",
                          "subject": "No signal after update",
                          "description": "Since this morning's update my phone shows no signal."
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/customer/requests/CT-[A-Z2-9]{6}")))
                .andExpect(jsonPath("$.referenceNumber", matchesPattern("CT-[A-HJKMNP-Z2-9]{6}")))
                .andExpect(jsonPath("$.customerName").value("Jordan Rivera"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.allowedNextStatuses", contains("IN_PROGRESS")))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.history", hasSize(1)))
                .andExpect(jsonPath("$.history[0].type").value("CREATED"))
                .andExpect(jsonPath("$.history[0].actorType").value("CUSTOMER"))
                .andExpect(jsonPath("$.history[0].newStatus").value("OPEN"));
    }

    @Test
    void createdRequestIsListedForTheDemoCustomerAndRetrievable() throws Exception {
        String ref = createRequest("BILLING");

        mvc.perform(get(CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].referenceNumber").value(ref))
                .andExpect(jsonPath("$[0].category").value("BILLING"))
                .andExpect(jsonPath("$[0].description").doesNotExist());

        mvc.perform(get(CUSTOMER + "/" + ref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Since this morning's update my phone shows no signal."));
    }

    @Test
    void referenceNumbersAreUnique() throws Exception {
        String first = createRequest("BILLING");
        String second = createRequest("BILLING");
        org.junit.jupiter.api.Assertions.assertNotEquals(first, second);
    }

    // ------------------------------------------------------------------ validation & errors

    @Test
    void invalidCreateReturns400WithFieldErrorsAndNoInternals() throws Exception {
        mvc.perform(post(CUSTOMER).contentType(APPLICATION_JSON).content("""
                        { "customerName": "Jordan", "subject": "   ", "description": "Short" }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.path").value(CUSTOMER))
                .andExpect(jsonPath("$.fieldErrors[*].field", contains("category", "description", "subject")))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());

        mvc.perform(get(CUSTOMER)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void overlongSubjectIsRejected() throws Exception {
        String subject = "x".repeat(121);
        mvc.perform(post(CUSTOMER).contentType(APPLICATION_JSON).content("""
                        { "customerName": "Jordan", "category": "BILLING",
                          "subject": "%s", "description": "A valid description." }
                        """.formatted(subject)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("subject"));
    }

    @Test
    void unknownCategoryAndMalformedJsonReturn400() throws Exception {
        mvc.perform(post(CUSTOMER).contentType(APPLICATION_JSON).content("""
                        { "customerName": "Jordan", "category": "ROAMING",
                          "subject": "Valid subject", "description": "A valid description." }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mvc.perform(post(CUSTOMER).contentType(APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON."));
    }

    @Test
    void missingRequestsReturn404Everywhere() throws Exception {
        mvc.perform(get(CUSTOMER + "/CT-MISS22"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No support request found with reference CT-MISS22."));
        mvc.perform(get(AGENT + "/CT-MISS22")).andExpect(status().isNotFound());
        mvc.perform(post(CUSTOMER + "/CT-MISS22/messages").contentType(APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(AGENT + "/CT-MISS22/updates").contentType(APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isNotFound());
        changeStatus("CT-MISS22", "IN_PROGRESS", "Starting work").andExpect(status().isNotFound());
    }

    @Test
    void customerViewCannotSeeAnotherCustomersRequest() throws Exception {
        Instant now = Instant.now();
        SupportRequest other = new SupportRequest("CT-200001", "DEMO-CUSTOMER-999", "Casey Morgan",
                RequestCategory.ORDER_SUPPORT, "Wrong item shipped", "I received the wrong charger.", now);
        other.addEvent(RequestEvent.created(other, "Casey Morgan", now));
        repository.save(other);

        mvc.perform(get(CUSTOMER + "/CT-200001")).andExpect(status().isNotFound());
        mvc.perform(get(CUSTOMER)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(AGENT + "/CT-200001")).andExpect(status().isOk());
    }

    @Test
    void unknownEndpointReturnsJson404() throws Exception {
        mvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Endpoint not found."));
    }

    // ------------------------------------------------------------------ status rules over HTTP

    @Test
    void forbiddenTransitionReturns409AndWritesNothing() throws Exception {
        String ref = createRequest("BILLING");

        changeStatus(ref, "RESOLVED", "Skipping ahead")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message",
                        containsString("Cannot change status from OPEN to RESOLVED")));
        changeStatus(ref, "OPEN", "Same status")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already OPEN")));

        // Neither the status nor the history changed: the rejected change was not partially saved.
        mvc.perform(get(AGENT + "/" + ref))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.history", hasSize(1)));
    }

    @Test
    void statusChangeRequiresAnExplanation() throws Exception {
        String ref = createRequest("BILLING");

        changeStatus(ref, "IN_PROGRESS", "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("explanation"));

        mvc.perform(get(AGENT + "/" + ref)).andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void customerMessagePreservesStatus() throws Exception {
        String ref = createRequest("SERVICE_ISSUE");
        changeStatus(ref, "IN_PROGRESS", "Looking into it").andExpect(status().isCreated());
        changeStatus(ref, "WAITING_ON_CUSTOMER", "Need your device model").andExpect(status().isCreated());

        mvc.perform(post(CUSTOMER + "/" + ref + "/messages").contentType(APPLICATION_JSON)
                        .content("{\"message\":\"It's a Nimbus X2.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING_ON_CUSTOMER"))
                .andExpect(jsonPath("$.history[3].type").value("CUSTOMER_MESSAGE"))
                .andExpect(jsonPath("$.history[3].message").value("It's a Nimbus X2."));
    }

    @Test
    void fullLifecycleFromCreationThroughResolutionAndReopening() throws Exception {
        String ref = createRequest("SERVICE_ISSUE");

        changeStatus(ref, "IN_PROGRESS", "Checking area network status")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allowedNextStatuses", contains("WAITING_ON_CUSTOMER", "RESOLVED")));
        changeStatus(ref, "WAITING_ON_CUSTOMER", "Please restart your device and confirm")
                .andExpect(status().isCreated());
        mvc.perform(post(CUSTOMER + "/" + ref + "/messages").contentType(APPLICATION_JSON)
                        .content("{\"message\":\"Restarted, still no signal.\"}"))
                .andExpect(status().isCreated());
        changeStatus(ref, "IN_PROGRESS", "Customer replied; escalating to network team")
                .andExpect(status().isCreated());
        mvc.perform(post(AGENT + "/" + ref + "/updates").contentType(APPLICATION_JSON)
                        .content("{\"message\":\"A tower repair is scheduled for tonight.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        changeStatus(ref, "RESOLVED", "Tower repaired; signal restored")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        changeStatus(ref, "IN_PROGRESS", "Customer reports the issue returned")
                .andExpect(status().isCreated());

        // Re-read from the database and check the whole timeline, in order.
        mvc.perform(get(CUSTOMER + "/" + ref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.history[*].type", contains(
                        "CREATED",
                        "STATUS_CHANGE",
                        "STATUS_CHANGE",
                        "CUSTOMER_MESSAGE",
                        "STATUS_CHANGE",
                        "AGENT_UPDATE",
                        "STATUS_CHANGE",
                        "REOPENED")))
                .andExpect(jsonPath("$.history[7].previousStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.history[7].newStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.history[7].message").value("Customer reports the issue returned"))
                .andExpect(jsonPath("$.history[7].actorType").value("AGENT"))
                .andExpect(jsonPath("$.history[7].actorName").value("Demo Agent"))
                .andExpect(jsonPath("$.history[7].createdAt").isNotEmpty());
    }

    // ------------------------------------------------------------------ agent list

    @Test
    void agentCanFilterByStatusAndCategory() throws Exception {
        String billing = createRequest("BILLING");
        String service = createRequest("SERVICE_ISSUE");
        createRequest("SERVICE_ISSUE");
        changeStatus(service, "IN_PROGRESS", "Starting work").andExpect(status().isCreated());

        mvc.perform(get(AGENT)).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get(AGENT).param("category", "BILLING"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].referenceNumber").value(billing));
        mvc.perform(get(AGENT).param("status", "IN_PROGRESS"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].referenceNumber").value(service));
        mvc.perform(get(AGENT).param("status", "OPEN").param("category", "SERVICE_ISSUE"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get(AGENT).param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Allowed values")));
    }

    @Test
    void listsAreOrderedByMostRecentActivity() throws Exception {
        String older = createRequest("BILLING");
        String newer = createRequest("BILLING");

        mvc.perform(get(AGENT)).andExpect(jsonPath("$[*].referenceNumber", contains(newer, older)));

        // Activity on the older request moves it to the top.
        mvc.perform(post(AGENT + "/" + older + "/updates").contentType(APPLICATION_JSON)
                .content("{\"message\":\"Update\"}")).andExpect(status().isCreated());

        mvc.perform(get(AGENT)).andExpect(jsonPath("$[*].referenceNumber", contains(older, newer)));
        mvc.perform(get(CUSTOMER)).andExpect(jsonPath("$[*].referenceNumber", hasItem(older)));
    }

    // ------------------------------------------------------------------ helpers

    private String createRequest(String category) throws Exception {
        String body = mvc.perform(post(CUSTOMER).contentType(APPLICATION_JSON).content("""
                        {
                          "customerName": "Jordan Rivera",
                          "category": "%s",
                          "subject": "No signal after update",
                          "description": "Since this morning's update my phone shows no signal."
                        }
                        """.formatted(category)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.referenceNumber");
    }

    private ResultActions changeStatus(String ref, String newStatus, String explanation) throws Exception {
        return mvc.perform(post(AGENT + "/" + ref + "/status-changes").contentType(APPLICATION_JSON)
                .content("""
                        { "newStatus": "%s", "explanation": "%s" }
                        """.formatted(newStatus, explanation)));
    }
}
