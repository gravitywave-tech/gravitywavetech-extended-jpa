package org.gravitywavetech.order.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.application.port.PaymentCreatePort;
import org.gravitywavetech.order.domain.model.Money;
import org.gravitywavetech.order.domain.model.OrderId;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * {@link PaymentCreatePort} 的 RabbitMQ 实现：向 payment 服务发送「创建支付单」消息。
 *
 * <p>跨服务消息契约 {@link PaymentCreateMessage} 的组装封装在本类，
 * 应用层只见领域类型（OrderId / Money），不感知消息格式与 binding 名。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCreateProducer implements PaymentCreatePort {

    private static final String OUTPUT = "paymentCreate-out-0";

    private final StreamBridge streamBridge;

    @Override
    public void requestCreatePayment(OrderId orderId, Money amount, String paymentMethod) {
        PaymentCreateMessage message = new PaymentCreateMessage(
                orderId.getId(),
                amount.getAmount(),
                paymentMethod,
                Instant.now()
        );
        String traceId = UUID.randomUUID().toString();
        Message<PaymentCreateMessage> outbound = MessageBuilder
                .withPayload(message)
                .setHeader("traceId", traceId)
                .build();
        log.info("发送支付单创建请求，traceId={}, orderId={}, amount={}",
                traceId, message.orderId(), message.amount());
        streamBridge.send(OUTPUT, outbound);
    }
}
