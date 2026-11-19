
package com.queueless.service;

import com.queueless.dto.QueuePositionResponse;
import com.queueless.entity.QueueToken;
import com.queueless.entity.Service;
import com.queueless.entity.TokenStatus;
import com.queueless.repository.QueueTokenRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
public class QueueTokenService {

    private final QueueTokenRepository tokenRepository;
    private final ServiceService serviceService;

    public QueueTokenService(
            QueueTokenRepository tokenRepository,
            ServiceService serviceService) {
        this.tokenRepository = tokenRepository;
        this.serviceService = serviceService;
    }

    @Transactional
    public QueueToken generateToken(String customerName, Long serviceId) {

        Service service = serviceService.getServiceForUpdate(serviceId);

        if (!service.isActive()) {
            throw new RuntimeException("This service is currently unavailable");
        }

        int nextTokenNumber = service.generateNextTokenNumber();

        QueueToken token = new QueueToken(
                nextTokenNumber, customerName, service);

        return tokenRepository.save(token);
    }

    public List<QueueToken> getWaitingTokens(Long serviceId) {
        return tokenRepository
                .findByServiceIdAndStatusOrderByTokenNumberAsc(
                        serviceId, TokenStatus.WAITING);
    }

    public QueuePositionResponse getQueuePosition(Long tokenId) {

        QueueToken token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        long peopleAhead = 0;

        if (token.getStatus() == TokenStatus.WAITING) {
            peopleAhead = tokenRepository
                    .countByServiceIdAndStatusAndTokenNumberLessThan(
                            token.getService().getId(),
                            TokenStatus.WAITING,
                            token.getTokenNumber());
        }

        return new QueuePositionResponse(
                token.getId(),
                token.getTokenNumber(),
                token.getCustomerName(),
                token.getStatus().name(),
                peopleAhead
        );
    }


    @Transactional
    public QueueToken callNext(Long serviceId) {

        Service service = serviceService.getServiceForUpdate(serviceId);

        if (!service.isActive()) {
            throw new RuntimeException("This service is currently unavailable");
        }

        if (tokenRepository.existsByServiceIdAndStatus(
                serviceId, TokenStatus.SERVING)) {
            throw new RuntimeException("A token is already being served");
        }

        List<QueueToken> waitingTokens = getWaitingTokens(serviceId);

        if (waitingTokens.isEmpty()) {
            throw new RuntimeException("No waiting tokens available");
        }

        QueueToken nextToken = waitingTokens.get(0);
        nextToken.setStatus(TokenStatus.SERVING);

        return tokenRepository.save(nextToken);
    }


    @Transactional
    public QueueToken completeToken(Long tokenId) {

        QueueToken token = getTokenForUpdate(tokenId);

        if (token.getStatus() != TokenStatus.SERVING) {
            throw new RuntimeException("Only serving tokens can be completed");
        }

        token.setStatus(TokenStatus.COMPLETED);
        return tokenRepository.save(token);
    }

    @Transactional
    public QueueToken skipToken(Long tokenId) {

        QueueToken token = getTokenForUpdate(tokenId);

        if (token.getStatus() != TokenStatus.SERVING) {
            throw new RuntimeException("Only serving tokens can be skipped");
        }

        token.setStatus(TokenStatus.SKIPPED);
        return tokenRepository.save(token);
    }

    private QueueToken getTokenForUpdate(Long tokenId) {
        return tokenRepository.findByIdForUpdate(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));
    }

    public QueueToken getCurrentServingToken(Long serviceId) {

        return tokenRepository
                .findFirstByServiceIdAndStatusOrderByTokenNumberAsc(
                        serviceId, TokenStatus.SERVING)
                .orElseThrow(() ->
                        new RuntimeException("No token is currently being served"));
    }

}
