
package com.queueless.service;

import com.queueless.entity.QueueToken;
import com.queueless.entity.Service;
import com.queueless.entity.TokenStatus;
import com.queueless.repository.QueueTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueTokenServiceTest {

    @Mock
    private QueueTokenRepository tokenRepository;

    @Mock
    private ServiceService serviceService;

    @Mock
    private Service service;

    @InjectMocks
    private QueueTokenService tokenService;

    @Test
    void generateTokenSuccessfully() {
        when(serviceService.getServiceForUpdate(1L))
                .thenReturn(service);
        when(service.isActive()).thenReturn(true);
        when(service.generateNextTokenNumber()).thenReturn(1);
        when(tokenRepository.save(any(QueueToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QueueToken token = tokenService.generateToken("Ahmed", 1L);

        assertEquals(1, token.getTokenNumber());
        assertEquals("Ahmed", token.getCustomerName());
        assertEquals(TokenStatus.WAITING, token.getStatus());

        verify(tokenRepository).save(any(QueueToken.class));
    }

    @Test
    void generateTokenShouldFailWhenServiceIsInactive() {
        when(serviceService.getServiceForUpdate(1L))
                .thenReturn(service);
        when(service.isActive()).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tokenService.generateToken("Ahmed", 1L)
        );

        assertEquals(
                "This service is currently unavailable",
                exception.getMessage()
        );

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void callNextShouldServeFirstWaitingToken() {
        when(serviceService.getServiceForUpdate(1L))
                .thenReturn(service);
        when(service.isActive()).thenReturn(true);
        when(tokenRepository.existsByServiceIdAndStatus(
                1L, TokenStatus.SERVING)).thenReturn(false);

        QueueToken waitingToken = mock(QueueToken.class);

        when(tokenRepository.findByServiceIdAndStatusOrderByTokenNumberAsc(
                1L, TokenStatus.WAITING))
                .thenReturn(List.of(waitingToken));

        when(tokenRepository.save(waitingToken))
                .thenReturn(waitingToken);

        QueueToken result = tokenService.callNext(1L);

        verify(waitingToken).setStatus(TokenStatus.SERVING);
        assertSame(waitingToken, result);
    }

    @Test
    void callNextShouldFailWhenQueueIsEmpty() {
        when(serviceService.getServiceForUpdate(1L))
                .thenReturn(service);
        when(service.isActive()).thenReturn(true);
        when(tokenRepository.existsByServiceIdAndStatus(
                1L, TokenStatus.SERVING)).thenReturn(false);

        when(tokenRepository.findByServiceIdAndStatusOrderByTokenNumberAsc(
                1L, TokenStatus.WAITING))
                .thenReturn(List.of());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tokenService.callNext(1L)
        );

        assertEquals("No waiting tokens available", exception.getMessage());
    }

    @Test
    void callNextShouldFailWhenAnotherTokenIsServing() {
        when(serviceService.getServiceForUpdate(1L))
                .thenReturn(service);
        when(service.isActive()).thenReturn(true);
        when(tokenRepository.existsByServiceIdAndStatus(
                1L, TokenStatus.SERVING)).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tokenService.callNext(1L)
        );

        assertEquals("A token is already being served", exception.getMessage());
    }


    @Test
    void completeTokenSuccessfully() {
        QueueToken token = new QueueToken(1, "Ahmed", service);
        token.setStatus(TokenStatus.SERVING);

        when(tokenRepository.findByIdForUpdate(1L))
                .thenReturn(java.util.Optional.of(token));

        when(tokenRepository.save(token)).thenReturn(token);

        QueueToken result = tokenService.completeToken(1L);

        assertEquals(TokenStatus.COMPLETED, result.getStatus());
        verify(tokenRepository).save(token);
    }

    @Test
    void completeTokenShouldFailWhenNotServing() {
        QueueToken token = new QueueToken(1, "Ahmed", service);
        token.setStatus(TokenStatus.WAITING);

        when(tokenRepository.findByIdForUpdate(1L))
                .thenReturn(java.util.Optional.of(token));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tokenService.completeToken(1L)
        );

        assertEquals(
                "Only serving tokens can be completed",
                exception.getMessage()
        );

        verify(tokenRepository, never()).save(any());
    }

    @Test
    void skipTokenSuccessfully() {
        QueueToken token = new QueueToken(1, "Ahmed", service);
        token.setStatus(TokenStatus.SERVING);

        when(tokenRepository.findByIdForUpdate(1L))
                .thenReturn(java.util.Optional.of(token));

        when(tokenRepository.save(token)).thenReturn(token);

        QueueToken result = tokenService.skipToken(1L);

        assertEquals(TokenStatus.SKIPPED, result.getStatus());
        verify(tokenRepository).save(token);
    }


    @Test
    void getQueuePositionSuccessfully() {
        QueueToken token = new QueueToken(2, "Bilal", service);
        token.setStatus(TokenStatus.WAITING);

        when(service.getId()).thenReturn(1L);

        when(tokenRepository.findById(2L))
                .thenReturn(java.util.Optional.of(token));

        when(tokenRepository.countByServiceIdAndStatusAndTokenNumberLessThan(
                1L, TokenStatus.WAITING, 2))
                .thenReturn(1L);

        var result = tokenService.getQueuePosition(2L);

        assertEquals(2, result.tokenNumber());
        assertEquals("Bilal", result.customerName());
        assertEquals("WAITING", result.status());
        assertEquals(1L, result.peopleAhead());
    }

    @Test
    void getQueuePositionShouldFailWhenTokenNotFound() {
        when(tokenRepository.findById(99L))
                .thenReturn(java.util.Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tokenService.getQueuePosition(99L)
        );

        assertEquals("Token not found", exception.getMessage());
    }

    @Test
    void getCurrentServingTokenSuccessfully() {
        QueueToken token = new QueueToken(1, "Ahmed", service);
        token.setStatus(TokenStatus.SERVING);

        when(tokenRepository.findFirstByServiceIdAndStatusOrderByTokenNumberAsc(
                1L, TokenStatus.SERVING))
                .thenReturn(java.util.Optional.of(token));

        QueueToken result = tokenService.getCurrentServingToken(1L);

        assertEquals("Ahmed", result.getCustomerName());
        assertEquals(TokenStatus.SERVING, result.getStatus());
    }

    @Test
    void getCurrentServingTokenShouldFailWhenNoneServing() {
        when(tokenRepository.findFirstByServiceIdAndStatusOrderByTokenNumberAsc(
                1L, TokenStatus.SERVING))
                .thenReturn(java.util.Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tokenService.getCurrentServingToken(1L)
        );

        assertEquals(
                "No token is currently being served",
                exception.getMessage()
        );
    }


}
