package org.gravitywavetech.payment.domain.exception;

/**
 * PaymentCreateException
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class PaymentCreateException extends RuntimeException {
    public PaymentCreateException(String msg) {
        super(msg);
    }
}
