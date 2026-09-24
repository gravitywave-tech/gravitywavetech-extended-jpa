package org.gravitywavetech.order.infrastructure.adapter;

import org.gravitywavetech.order.domain.model.Money;

/**
 * PaymentGateway
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public interface PaymentGateway {
    PaymentResult requestPayment(PaymentRequest req);

    record PaymentRequest(String orderRef, Money amount, String payMethod){}
    record PaymentResult(String transactionId, boolean success){}
}
