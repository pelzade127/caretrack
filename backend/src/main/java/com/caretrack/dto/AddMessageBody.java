package com.caretrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.caretrack.dto.ValidationLimits.MESSAGE_MAX;

/** JSON body for a customer message or an agent's customer-visible update. */
public record AddMessageBody(
        @NotBlank(message = "Message is required.")
        @Size(max = MESSAGE_MAX, message = "Message must be at most {max} characters.")
        String message
) {
    public AddMessageBody {
        message = ValidationLimits.clean(message);
    }
}
