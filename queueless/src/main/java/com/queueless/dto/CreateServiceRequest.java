
package com.queueless.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateServiceRequest(

        @NotBlank(message = "Service name is required")
        String name

) {
}
