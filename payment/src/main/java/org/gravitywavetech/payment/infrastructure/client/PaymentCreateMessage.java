package org.gravitywavetech.payment.infrastructure.client;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * order 服务发来的「创建支付单」消息体。
 */
public record PaymentCreateMessage(
        Long orderId,
        BigDecimal amount,
        String paymentMethod,
        Instant requestedAt
) {
}
