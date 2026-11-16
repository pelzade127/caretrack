package com.caretrack.dto;

import com.caretrack.entity.RequestCategory;
import com.caretrack.entity.RequestStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.caretrack.dto.ValidationLimits.CUSTOMER_NAME_MAX;
import static com.caretrack.dto.ValidationLimits.DESCRIPTION_MAX;
import static com.caretrack.dto.ValidationLimits.DESCRIPTION_MIN;
import static com.caretrack.dto.ValidationLimits.EXPLANATION_MAX;
import static com.caretrack.dto.ValidationLimits.MESSAGE_MAX;
import static com.caretrack.dto.ValidationLimits.SUBJECT_MAX;
import static com.caretrack.dto.ValidationLimits.SUBJECT_MIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bean Validation rules on the request bodies, checked with a plain Validator (no Spring).
 * Boundary tests use exactly the limit (valid) and one past it (invalid).
 */
class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // ------------------------------------------------------------------ create request

    @Test
    void validCreateBodyPasses() {
        assertEquals(Set.of(), invalidFields(validCreate()));
    }

    @Test
    void allCreateFieldsAreRequired() {
        CreateRequestBody body = new CreateRequestBody(null, null, null, null);
        assertEquals(Set.of("customerName", "category", "subject", "description"), invalidFields(body));
    }

    @Test
    void whitespaceOnlyCountsAsMissingAndIsReportedOncePerField() {
        CreateRequestBody body = new CreateRequestBody("   ", RequestCategory.BILLING, " \t ", "\n\n");

        List<String> messages = validator.validate(body).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();

        assertEquals(List.of("Customer name is required.", "Description is required.", "Subject is required."),
                messages);
    }

    @Test
    void surroundingWhitespaceIsTrimmedBeforeValidation() {
        CreateRequestBody body = new CreateRequestBody("  Jordan  ", RequestCategory.BILLING,
                "   Hi!  ", "  Ten chars!  ");

        assertEquals("Jordan", body.customerName());
        // "Hi!" is 3 characters once trimmed, below the minimum of 5, even though the raw input was longer.
        assertEquals(Set.of("subject"), invalidFields(body));
    }

    @Test
    void subjectLengthLimits() {
        assertEquals(Set.of(), invalidFields(createWithSubject("x".repeat(SUBJECT_MIN))));
        assertEquals(Set.of(), invalidFields(createWithSubject("x".repeat(SUBJECT_MAX))));
        assertEquals(Set.of("subject"), invalidFields(createWithSubject("x".repeat(SUBJECT_MIN - 1))));
        assertEquals(Set.of("subject"), invalidFields(createWithSubject("x".repeat(SUBJECT_MAX + 1))));
    }

    @Test
    void descriptionLengthLimits() {
        assertEquals(Set.of(), invalidFields(createWithDescription("x".repeat(DESCRIPTION_MAX))));
        assertEquals(Set.of("description"), invalidFields(createWithDescription("x".repeat(DESCRIPTION_MIN - 1))));
        assertEquals(Set.of("description"), invalidFields(createWithDescription("x".repeat(DESCRIPTION_MAX + 1))));
    }

    @Test
    void customerNameLengthLimit() {
        CreateRequestBody atLimit = new CreateRequestBody("x".repeat(CUSTOMER_NAME_MAX), RequestCategory.BILLING,
                "Valid subject", "A valid description.");
        CreateRequestBody overLimit = new CreateRequestBody("x".repeat(CUSTOMER_NAME_MAX + 1),
                RequestCategory.BILLING, "Valid subject", "A valid description.");

        assertEquals(Set.of(), invalidFields(atLimit));
        assertEquals(Set.of("customerName"), invalidFields(overLimit));
    }

    // ------------------------------------------------------------------ messages

    @Test
    void messageMustBePresentAndWithinLimit() {
        assertEquals(Set.of("message"), invalidFields(new AddMessageBody(null)));
        assertEquals(Set.of("message"), invalidFields(new AddMessageBody("    ")));
        assertEquals(Set.of(), invalidFields(new AddMessageBody("x".repeat(MESSAGE_MAX))));
        assertEquals(Set.of("message"), invalidFields(new AddMessageBody("x".repeat(MESSAGE_MAX + 1))));
    }

    // ------------------------------------------------------------------ status change

    @Test
    void statusChangeRequiresStatusAndExplanation() {
        assertEquals(Set.of("newStatus", "explanation"), invalidFields(new ChangeStatusBody(null, "   ")));
    }

    @Test
    void explanationLengthLimit() {
        assertEquals(Set.of(),
                invalidFields(new ChangeStatusBody(RequestStatus.IN_PROGRESS, "x".repeat(EXPLANATION_MAX))));
        assertEquals(Set.of("explanation"),
                invalidFields(new ChangeStatusBody(RequestStatus.IN_PROGRESS, "x".repeat(EXPLANATION_MAX + 1))));
        assertEquals(Set.of("explanation"),
                invalidFields(new ChangeStatusBody(RequestStatus.IN_PROGRESS, "ok")));
    }

    // ------------------------------------------------------------------ helpers

    private static CreateRequestBody validCreate() {
        return new CreateRequestBody("Jordan Rivera", RequestCategory.SERVICE_ISSUE,
                "No signal at home", "No signal since this morning's update.");
    }

    private static CreateRequestBody createWithSubject(String subject) {
        return new CreateRequestBody("Jordan Rivera", RequestCategory.BILLING, subject, "A valid description.");
    }

    private static CreateRequestBody createWithDescription(String description) {
        return new CreateRequestBody("Jordan Rivera", RequestCategory.BILLING, "Valid subject", description);
    }

    private static Set<String> invalidFields(Object body) {
        Set<String> fields = validator.validate(body).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
        assertTrue(fields.stream().noneMatch(String::isBlank), "every violation should name a field");
        return fields;
    }
}
