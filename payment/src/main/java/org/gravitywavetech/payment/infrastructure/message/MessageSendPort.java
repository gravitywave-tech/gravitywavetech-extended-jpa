package org.gravitywavetech.payment.infrastructure.message;

import org.gravitywavetech.payment.domain.message.OutboxMessage;

/**
 * 消息发送端口（Hexagonal Architecture 的驱动适配器端口）。
 *
 * <p>Outbox 领域不感知具体消息中间件；由实现决定是 RabbitMQ、Kafka、HTTP 等。
 * 端口签名只接受 Outbox 领域对象，把序列化细节留在实现。</p>
 */
public interface MessageSendPort {

    /**
     * 将 Outbox 消息投递到消息中间件。
     *
     * @param message 待投递消息（状态必须为 PENDING 或重试中的 PENDING）
     * @throws MessageSendException 投递失败时抛出，由 Dispatcher 捕获并触发重试
     */
    void send(OutboxMessage message);
}
