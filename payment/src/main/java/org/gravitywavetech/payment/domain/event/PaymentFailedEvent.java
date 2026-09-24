package org.gravitywavetech.payment.domain.event;

import org.gravitywavetech.payment.domain.model.OrderRef;
import org.gravitywavetech.payment.domain.model.PaymentId;

import java.time.Instant;

/**
 * PaymentFailedEvent
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class PaymentFailedEvent {
    private final PaymentId paymentId;
    private final OrderRef orderRef;
    private final String failReason;
    private final Instant occurAt;

    public PaymentFailedEvent(PaymentId paymentId, OrderRef orderRef, String failReason, Instant occurAt) {
        this.paymentId = paymentId;
        this.orderRef = orderRef;
        this.failReason = failReason;
        this.occurAt = occurAt;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public OrderRef getOrderRef() { return orderRef; }
    public String getFailReason() { return failReason; }
    public Instant getOccurAt() { return occurAt; }
}

