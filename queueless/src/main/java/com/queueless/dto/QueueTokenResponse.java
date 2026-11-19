
package com.queueless.dto;

import com.queueless.entity.TokenStatus;

import java.time.LocalDateTime;

public record QueueTokenResponse(
        Long id,
        Integer tokenNumber,
        String customerName,
        TokenStatus status,
        LocalDateTime createdAt,
        Long serviceId,
        String serviceName
) {
}
