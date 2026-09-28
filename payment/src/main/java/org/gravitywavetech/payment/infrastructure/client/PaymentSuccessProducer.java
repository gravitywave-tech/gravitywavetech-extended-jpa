package org.gravitywavetech.payment.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 向 order 服务发送支付成功回调。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessProducer {

    private static final String OUTPUT = "paymentSuccess-out-0";

    private final StreamBridge streamBridge;

    public void sendPaymentSuccess(PaymentSuccessMessage message) {
        String traceId = UUID.randomUUID().toString();
        Message<PaymentSuccessMessage> outbound = MessageBuilder
                .withPayload(message)
                .setHeader("traceId", traceId)
                .build();
        log.info("发送支付成功回调，traceId={}, paymentId={}, orderId={}",
                traceId, message.paymentId(), message.orderId());
        streamBridge.send(OUTPUT, outbound);
    }
}
