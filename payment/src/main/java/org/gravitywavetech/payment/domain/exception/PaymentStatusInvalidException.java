package org.gravitywavetech.payment.domain.exception;

/**
 * PaymentStatusInvalidException
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class PaymentStatusInvalidException extends RuntimeException {
    public PaymentStatusInvalidException(String msg) {
        super(msg);
    }
}

