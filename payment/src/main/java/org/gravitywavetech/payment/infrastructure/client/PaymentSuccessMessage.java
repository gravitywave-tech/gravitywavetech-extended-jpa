package org.gravitywavetech.payment.infrastructure.client;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * payment 服务向 order 服务发出的「支付成功」消息体。
 */
public record PaymentSuccessMessage(
        String paymentId,
        Long orderId,
        BigDecimal paidAmount,
        String thirdPartyTradeNo,
        Instant paidAt
) {
}
