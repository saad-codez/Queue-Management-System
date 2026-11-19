
package com.queueless.dto;

public record QueuePositionResponse(
        Long tokenId,
        Integer tokenNumber,
        String customerName,
        String status,
        long peopleAhead
) {
}
