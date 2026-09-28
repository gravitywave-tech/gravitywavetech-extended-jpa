package org.gravitywavetech.payment.infrastructure.message.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import org.gravitywavetech.payment.domain.message.OutboxStatus;

import java.time.Instant;

/**
 * Outbox 本地消息表实体。
 *
 * <p>关键约束：
 * <ul>
 *   <li>{@code messageKey} 唯一索引：业务侧幂等</li>
 *   <li>{@code status} + {@code next_retry_time} 联合索引：Dispatcher 扫描性能</li>
 *   <li>{@code @Version}：乐观锁，避免多实例并发重复投递</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "t_outbox_message", indexes = {
        @Index(name = "uk_outbox_message_key", columnList = "message_key", unique = true),
        @Index(name = "idx_outbox_status_retry", columnList = "status, next_retry_time")
})
@Data
public class OutboxMessageEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "message_key", nullable = false, length = 128)
    private String messageKey;

    /** Spring Cloud Stream output binding name，如 paymentSuccess-out-0。 */
    @Column(name = "destination", nullable = false, length = 64)
    private String destination;

    @Column(name = "routing_key", length = 128)
    private String routingKey;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_time", nullable = false)
    private Instant nextRetryTime;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    @Version
    @Column(name = "version")
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
