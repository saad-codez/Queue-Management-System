
package com.queueless.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTokenRequest(

        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotNull(message = "Service ID is required")
        @Positive(message = "Service ID must be positive")
        Long serviceId

) {
}
