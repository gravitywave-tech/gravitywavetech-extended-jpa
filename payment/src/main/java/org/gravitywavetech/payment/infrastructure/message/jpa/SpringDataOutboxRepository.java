package org.gravitywavetech.payment.infrastructure.message.jpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.payment.domain.message.OutboxMessage;
import org.gravitywavetech.payment.domain.message.OutboxMessageId;
import org.gravitywavetech.payment.domain.message.OutboxRepository;
import org.gravitywavetech.payment.domain.message.OutboxStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbox 仓储适配器：连接领域仓储与 JPA。
 *
 * <p>关键设计：{@link #save} 采用 {@code Propagation.MANDATORY}，强制调用方处于
 * 事务上下文 —— 这是 Outbox 模式的核心保证：消息与业务状态原子提交。</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SpringDataOutboxRepository implements OutboxRepository {

    private final OutboxMessageJpaRepository jpaRepository;

    /** 与业务事务同事务，保证消息与业务状态一起提交/回滚。 */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxMessage save(OutboxMessage message) {
        if (message == null || message.getId() == null) {
            throw new IllegalArgumentException("OutboxMessage 必须包含 id");
        }
        if (message.getStatus() != OutboxStatus.PENDING) {
            throw new IllegalStateException("新建 Outbox 必须处于 PENDING 状态，当前=" + message.getStatus());
        }
        // 幂等：若同 messageKey 已存在则不重复写入
        Optional<OutboxMessageEntity> existing = jpaRepository.findByMessageKey(message.getMessageKey());
        if (existing.isPresent()) {
            log.warn("Outbox 消息已存在，跳过写入，messageKey={}", message.getMessageKey());
            return toDomain(existing.get());
        }

        OutboxMessageEntity entity = new OutboxMessageEntity();
        entity.setId(message.getId().getId());
        entity.setMessageKey(message.getMessageKey());
        entity.setDestination(message.getDestination());
        entity.setRoutingKey(message.getRoutingKey());
        entity.setPayload(message.getPayload());
        entity.setStatus(OutboxStatus.PENDING);
        entity.setRetryCount(0);
        entity.setNextRetryTime(Instant.now());
        entity.setCreatedAt(Instant.now());
        jpaRepository.save(entity);
        return message;
    }

    @Override
    public Optional<OutboxMessage> findById(OutboxMessageId id) {
        return jpaRepository.findById(id.getId()).map(SpringDataOutboxRepository::toDomain);
    }

    @Override
    public Optional<OutboxMessage> findByMessageKey(String messageKey) {
        return jpaRepository.findByMessageKey(messageKey).map(SpringDataOutboxRepository::toDomain);
    }

    @Override
    public List<OutboxMessage> findDueToSend(Instant now, int limit) {
        return jpaRepository.findDueToSend(now, PageRequest.of(0, limit))
                .stream()
                .map(SpringDataOutboxRepository::toDomain)
                .toList();
    }

    /**
     * 状态更新走独立事务，避免长时间持有业务事务锁。
     * @Version 乐观锁失败会抛出 OptimisticLockException，Dispatcher 捕获后跳过。
     */
    @Override
    @Transactional
    public OutboxMessage update(OutboxMessage message) {
        return jpaRepository.findById(message.getId().getId())
                .map(entity -> {
                    entity.setStatus(message.getStatus());
                    entity.setRetryCount(message.getRetryCount());
                    entity.setNextRetryTime(message.getNextRetryTime());
                    entity.setSentAt(message.getSentAt());
                    entity.setLastError(truncate(message.getLastError(), 1024));
                    return toDomain(jpaRepository.save(entity));
                })
                .orElseThrow(() -> new IllegalStateException("Outbox 消息不存在，id=" + message.getId()));
    }

    private static OutboxMessage toDomain(OutboxMessageEntity entity) {
        return OutboxMessage.rehydrate(
                new OutboxMessageId(entity.getId()),
                entity.getMessageKey(),
                entity.getDestination(),
                entity.getRoutingKey(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getNextRetryTime(),
                entity.getSentAt(),
                entity.getLastError()
        );
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
