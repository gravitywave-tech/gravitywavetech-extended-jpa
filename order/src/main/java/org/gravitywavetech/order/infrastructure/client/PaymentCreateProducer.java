package org.gravitywavetech.order.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * 向 payment 服务发送创建支付单请求。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCreateProducer {

    private static final String OUTPUT = "paymentCreate-out-0";

    private final StreamBridge streamBridge;

    public void sendCreatePayment(PaymentCreateMessage message) {
        String traceId = UUID.randomUUID().toString();
        Message<PaymentCreateMessage> outbound = MessageBuilder
                .withPayload(message)
                .setHeader("traceId", traceId)
                .build();
        log.info("发送支付单创建请求，traceId={}, orderId={}, amount={}",
                traceId, message.orderId(), message.amount());
        streamBridge.send(OUTPUT, outbound);
    }

    public static PaymentCreateMessage of(Long orderId, java.math.BigDecimal amount, String paymentMethod) {
        return new PaymentCreateMessage(orderId, amount, paymentMethod, Instant.now());
    }
}
