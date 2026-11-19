
package com.queueless.controller;

import com.queueless.dto.QueueTokenResponse;
import com.queueless.entity.QueueToken;
import com.queueless.service.QueueTokenService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queues")
public class QueueManagementController {

    private final QueueTokenService tokenService;

    public QueueManagementController(QueueTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/{serviceId}/next")
    public QueueTokenResponse callNext(@PathVariable Long serviceId) {
        return toResponse(tokenService.callNext(serviceId));
    }

    @GetMapping("/{serviceId}/current")
    public QueueTokenResponse getCurrentServingToken(
            @PathVariable Long serviceId) {
        return toResponse(tokenService.getCurrentServingToken(serviceId));
    }

    @PutMapping("/tokens/{tokenId}/complete")
    public QueueTokenResponse completeToken(@PathVariable Long tokenId) {
        return toResponse(tokenService.completeToken(tokenId));
    }

    @PutMapping("/tokens/{tokenId}/skip")
    public QueueTokenResponse skipToken(@PathVariable Long tokenId) {
        return toResponse(tokenService.skipToken(tokenId));
    }

    private QueueTokenResponse toResponse(QueueToken token) {
        return new QueueTokenResponse(
                token.getId(),
                token.getTokenNumber(),
                token.getCustomerName(),
                token.getStatus(),
                token.getCreatedAt(),
                token.getService().getId(),
                token.getService().getName()
        );
    }
}
