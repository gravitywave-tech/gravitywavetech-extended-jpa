package org.gravitywavetech.payment.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.payment.domain.message.OutboxMessage;
import org.gravitywavetech.payment.domain.message.OutboxRepository;
import org.gravitywavetech.payment.infrastructure.message.OutboxJsonSupport;
import org.springframework.stereotype.Service;

/**
 * Outbox 应用服务：编排「业务事务内落 Outbox」这一横切能力。
 *
 * <p>关键约定：本服务的所有方法必须在事务上下文中调用（由 {@code PaymentApplicationService} 的
 * {@code @Transactional} 保证）。{@code OutboxRepository.save} 使用
 * {@code Propagation.MANDATORY} 强制这一约束，事务缺失会立即失败。</p>
 *
 * <p>调用点示例：{@code PaymentApplicationService.handlePaymentCallback} 中把
 * {@code Payment.markSuccess} 状态变更与 outbox 写入放在同一事务，
 * 保证「支付成功 + 消息待投递」的一致性。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final OutboxJsonSupport jsonSupport;

    /**
     * 在业务事务内写入一条待投递消息。
     *
     * @param messageKey  业务幂等键（推荐: 事件类型 + 业务主键）
     * @param destination Spring Cloud Stream output binding 名（如 paymentSuccess-out-0）
     * @param payload     待序列化消息体
     * @return 已持久化的 OutboxMessage（状态 = PENDING）
     */
    public <T> OutboxMessage publish(String messageKey, String destination, T payload) {
        return publish(messageKey, destination, null, payload);
    }

    public <T> OutboxMessage publish(String messageKey, String destination, String routingKey, T payload) {
        String json = jsonSupport.toJson(payload);
        OutboxMessage message = OutboxMessage.create(
                OutboxMessage.newId(),
                messageKey,
                destination,
                routingKey,
                json
        );
        OutboxMessage saved = outboxRepository.save(message);
        log.info("Outbox 消息已落库，messageKey={}, destination={}", messageKey, destination);
        return saved;
    }
}
