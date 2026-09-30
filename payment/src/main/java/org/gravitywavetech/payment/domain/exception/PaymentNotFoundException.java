package org.gravitywavetech.payment.domain.exception;

/**
 * 支付单不存在。
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/30
 */
public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Long paymentId) {
        super("支付单不存在，paymentId=" + paymentId);
    }
}
