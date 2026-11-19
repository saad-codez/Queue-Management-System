
package com.queueless.concurrency;

import com.queueless.entity.QueueToken;
import com.queueless.entity.Service;
import com.queueless.entity.TokenStatus;
import com.queueless.repository.QueueTokenRepository;
import com.queueless.repository.ServiceRepository;
import com.queueless.service.QueueTokenService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QueueConcurrencyIntegrationTest {

    @Autowired
    private QueueTokenService tokenService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private QueueTokenRepository tokenRepository;

    private Long createService() {
        Service service = new Service(
                "Concurrency-" + UUID.randomUUID()
        );

        return serviceRepository.saveAndFlush(service).getId();
    }

    private void waitForStart(
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {

        ready.countDown();
        start.await();
    }

    @Test
    void concurrentTokenGenerationShouldProduceUniqueNumbers()
            throws Exception {

        Long serviceId = createService();

        int numberOfRequests = 20;

        ExecutorService executor =
                Executors.newFixedThreadPool(numberOfRequests);

        CountDownLatch ready =
                new CountDownLatch(numberOfRequests);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<QueueToken>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < numberOfRequests; i++) {

                final int customerNumber = i;

                futures.add(executor.submit(() -> {
                    waitForStart(ready, start);

                    return tokenService.generateToken(
                            "Customer-" + customerNumber,
                            serviceId
                    );
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            List<Integer> tokenNumbers = new ArrayList<>();

            for (Future<QueueToken> future : futures) {
                tokenNumbers.add(
                        future.get(30, TimeUnit.SECONDS).getTokenNumber()
                );
            }

            Collections.sort(tokenNumbers);

            assertEquals(numberOfRequests, tokenNumbers.size());

            for (int i = 0; i < numberOfRequests; i++) {
                assertEquals(i + 1, tokenNumbers.get(i));
            }

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentCallNextShouldAllowOnlyOneServingToken()
            throws Exception {

        Long serviceId = createService();

        int numberOfTokens = 10;

        for (int i = 0; i < numberOfTokens; i++) {
            tokenService.generateToken("Customer-" + i, serviceId);
        }

        ExecutorService executor =
                Executors.newFixedThreadPool(numberOfTokens);

        CountDownLatch ready =
                new CountDownLatch(numberOfTokens);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < numberOfTokens; i++) {

                futures.add(executor.submit(() -> {
                    waitForStart(ready, start);

                    try {
                        tokenService.callNext(serviceId);
                        return true;
                    } catch (RuntimeException exception) {
                        return false;
                    }
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int successfulCalls = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) {
                    successfulCalls++;
                }
            }

            assertEquals(1, successfulCalls);

            List<QueueToken> servingTokens =
                    tokenRepository
                            .findByServiceIdAndStatusOrderByTokenNumberAsc(
                                    serviceId,
                                    TokenStatus.SERVING
                            );

            assertEquals(1, servingTokens.size());

            List<QueueToken> waitingTokens =
                    tokenRepository
                            .findByServiceIdAndStatusOrderByTokenNumberAsc(
                                    serviceId,
                                    TokenStatus.WAITING
                            );

            assertEquals(9, waitingTokens.size());

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void differentServicesShouldMaintainIndependentTokenSequences()
            throws Exception {

        Long firstServiceId = createService();
        Long secondServiceId = createService();

        int requestsPerService = 10;

        ExecutorService executor =
                Executors.newFixedThreadPool(20);

        CountDownLatch ready = new CountDownLatch(20);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<QueueToken>> firstServiceFutures =
                new ArrayList<>();

        List<Future<QueueToken>> secondServiceFutures =
                new ArrayList<>();

        try {
            for (int i = 0; i < requestsPerService; i++) {

                final int customerNumber = i;

                firstServiceFutures.add(executor.submit(() -> {
                    waitForStart(ready, start);

                    return tokenService.generateToken(
                            "First-" + customerNumber,
                            firstServiceId
                    );
                }));

                secondServiceFutures.add(executor.submit(() -> {
                    waitForStart(ready, start);

                    return tokenService.generateToken(
                            "Second-" + customerNumber,
                            secondServiceId
                    );
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            List<Integer> firstNumbers = new ArrayList<>();
            List<Integer> secondNumbers = new ArrayList<>();

            for (Future<QueueToken> future : firstServiceFutures) {
                firstNumbers.add(
                        future.get(30, TimeUnit.SECONDS).getTokenNumber()
                );
            }

            for (Future<QueueToken> future : secondServiceFutures) {
                secondNumbers.add(
                        future.get(30, TimeUnit.SECONDS).getTokenNumber()
                );
            }

            Collections.sort(firstNumbers);
            Collections.sort(secondNumbers);

            assertEquals(10, firstNumbers.size());
            assertEquals(10, secondNumbers.size());

            for (int i = 0; i < requestsPerService; i++) {
                assertEquals(i + 1, firstNumbers.get(i));
                assertEquals(i + 1, secondNumbers.get(i));
            }

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
