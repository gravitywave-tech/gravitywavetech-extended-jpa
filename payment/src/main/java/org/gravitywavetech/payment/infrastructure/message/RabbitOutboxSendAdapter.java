package org.gravitywavetech.payment.infrastructure.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.payment.domain.message.OutboxMessage;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 发送适配器（通过 Spring Cloud Stream + StreamBridge）。
 *
 * <p>Outbox 中的 payload 已经是 JSON 字符串，此处直接使用 String 作为消息 payload；
 * RabbitMQ 端 Spring Cloud Stream 使用默认 JSON converter 反序列化到消费方 record。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitOutboxSendAdapter implements MessageSendPort {

    private final StreamBridge streamBridge;

    @Override
    public void send(OutboxMessage message) {
        String destination = message.getDestination();
        String traceId = message.getMessageKey();
        Message<String> outbound = MessageBuilder
                .withPayload(message.getPayload())
                .setHeader("traceId", traceId)
                .setHeader("outboxMessageId", message.getId().getId())
                .build();
        log.info("Outbox 发送消息，destination={}, messageKey={}, retryCount={}",
                destination, message.getMessageKey(), message.getRetryCount());
        try {
            streamBridge.send(destination, outbound);
        } catch (Exception e) {
            throw new MessageSendException("Outbox 消息投递失败，destination=" + destination
                    + ", messageKey=" + message.getMessageKey(), e);
        }
    }
}
