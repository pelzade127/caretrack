package com.caretrack.dto;

import com.caretrack.entity.RequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static com.caretrack.dto.ValidationLimits.EXPLANATION_MAX;
import static com.caretrack.dto.ValidationLimits.EXPLANATION_MIN;

/**
 * JSON body for {@code POST /api/agent/requests/{reference}/status-changes}.
 * Validation here only checks the shape of the input; whether the transition is
 * allowed is decided by the service.
 */
public record ChangeStatusBody(
        @NotNull(message = "New status is required.")
        RequestStatus newStatus,

        @NotBlank(message = "An explanation is required for every status change.")
        @Size(min = EXPLANATION_MIN, max = EXPLANATION_MAX, message = "Explanation must be {min}–{max} characters.")
        String explanation
) {
    public ChangeStatusBody {
        explanation = ValidationLimits.clean(explanation);
    }
}
