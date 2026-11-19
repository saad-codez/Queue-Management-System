
package com.queueless.repository;

import com.queueless.entity.QueueToken;
import com.queueless.entity.TokenStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QueueTokenRepository extends JpaRepository<QueueToken, Long> {

    List<QueueToken> findByServiceIdAndStatusOrderByTokenNumberAsc(
            Long serviceId, TokenStatus status);

    Optional<QueueToken> findFirstByServiceIdAndStatusOrderByTokenNumberAsc(
            Long serviceId, TokenStatus status);

    boolean existsByServiceIdAndStatus(Long serviceId, TokenStatus status);

    long countByServiceIdAndStatusAndTokenNumberLessThan(
            Long serviceId, TokenStatus status, Integer tokenNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM QueueToken t WHERE t.id = :id")
    Optional<QueueToken> findByIdForUpdate(@Param("id") Long id);
}
