package org.gravitywavetech.order.infrastructure.client;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * payment 服务回调 order 服务的「支付成功」消息体。
 */
public record PaymentSuccessMessage(
        String paymentId,
        Long orderId,
        BigDecimal paidAmount,
        String thirdPartyTradeNo,
        Instant paidAt
) {
}
