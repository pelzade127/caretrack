package com.caretrack.dto;

import com.caretrack.entity.RequestCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.caretrack.dto.ValidationLimits.CUSTOMER_NAME_MAX;
import static com.caretrack.dto.ValidationLimits.DESCRIPTION_MAX;
import static com.caretrack.dto.ValidationLimits.DESCRIPTION_MIN;
import static com.caretrack.dto.ValidationLimits.SUBJECT_MAX;
import static com.caretrack.dto.ValidationLimits.SUBJECT_MIN;

/**
 * JSON body for {@code POST /api/customer/requests}.
 *
 * The compact constructor runs when Jackson builds the record, so values are trimmed
 * <em>before</em> Bean Validation checks them.
 */
public record CreateRequestBody(
        @NotBlank(message = "Customer name is required.")
        @Size(max = CUSTOMER_NAME_MAX, message = "Customer name must be at most {max} characters.")
        String customerName,

        @NotNull(message = "Category is required.")
        RequestCategory category,

        @NotBlank(message = "Subject is required.")
        @Size(min = SUBJECT_MIN, max = SUBJECT_MAX, message = "Subject must be {min}–{max} characters.")
        String subject,

        @NotBlank(message = "Description is required.")
        @Size(min = DESCRIPTION_MIN, max = DESCRIPTION_MAX, message = "Description must be {min}–{max} characters.")
        String description
) {
    public CreateRequestBody {
        customerName = ValidationLimits.clean(customerName);
        subject = ValidationLimits.clean(subject);
        description = ValidationLimits.clean(description);
    }
}
