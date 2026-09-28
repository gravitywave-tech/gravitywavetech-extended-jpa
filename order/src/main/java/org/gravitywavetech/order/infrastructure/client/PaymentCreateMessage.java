package org.gravitywavetech.order.infrastructure.client;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * order 服务向 payment 服务发出的「创建支付单」消息体。
 *
 * <p>跨服务契约以 JSON 序列化，字段保持最小化。</p>
 */
public record PaymentCreateMessage(
        Long orderId,
        BigDecimal amount,
        String paymentMethod,
        Instant requestedAt
) {
}
