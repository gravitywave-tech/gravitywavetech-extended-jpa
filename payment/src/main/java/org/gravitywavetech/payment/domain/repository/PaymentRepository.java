package org.gravitywavetech.payment.domain.repository;

import org.gravitywavetech.payment.domain.exception.PaymentCreateException;
import org.gravitywavetech.payment.domain.model.Payment;
import org.gravitywavetech.payment.domain.model.PaymentId;

/**
 * PaymentRepository
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public interface PaymentRepository {
    Payment findById(PaymentId paymentId);
    void save(Payment payment) throws PaymentCreateException;
}

