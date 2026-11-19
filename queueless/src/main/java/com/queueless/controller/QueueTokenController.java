
package com.queueless.controller;

import com.queueless.dto.CreateTokenRequest;
import com.queueless.dto.QueuePositionResponse;
import com.queueless.dto.QueueTokenResponse;
import com.queueless.entity.QueueToken;
import com.queueless.service.QueueTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tokens")
public class QueueTokenController {

    private final QueueTokenService tokenService;

    public QueueTokenController(QueueTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QueueTokenResponse generateToken(
            @Valid @RequestBody CreateTokenRequest request) {

        QueueToken token = tokenService.generateToken(
                request.customerName(),
                request.serviceId()
        );

        return toResponse(token);
    }

    @GetMapping("/waiting/{serviceId}")
    public List<QueueTokenResponse> getWaitingTokens(
            @PathVariable Long serviceId) {

        return tokenService.getWaitingTokens(serviceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{tokenId}/position")
    public QueuePositionResponse getQueuePosition(
            @PathVariable Long tokenId) {
        return tokenService.getQueuePosition(tokenId);
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
