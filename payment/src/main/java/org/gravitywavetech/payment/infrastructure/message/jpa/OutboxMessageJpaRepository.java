package org.gravitywavetech.payment.infrastructure.message.jpa;

import org.gravitywavetech.extended.jpa.repository.ExtendedBaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbox JPA 仓储。
 */
public interface OutboxMessageJpaRepository
        extends ExtendedBaseRepository<OutboxMessageEntity, Long> {

    Optional<OutboxMessageEntity> findByMessageKey(String messageKey);

    @Query("SELECT e FROM OutboxMessageEntity e " +
            "WHERE e.status = 'PENDING' AND e.nextRetryTime <= :now " +
            "ORDER BY e.nextRetryTime ASC")
    List<OutboxMessageEntity> findDueToSend(@Param("now") Instant now,
                                            org.springframework.data.domain.Pageable pageable);
}
