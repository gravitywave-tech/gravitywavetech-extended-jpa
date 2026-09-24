package org.gravitywavetech.payment.domain.event;

import org.gravitywavetech.payment.domain.model.Money;
import org.gravitywavetech.payment.domain.model.OrderRef;
import org.gravitywavetech.payment.domain.model.PaymentId;

import java.time.Instant;

/**
 * PaymentCreatedEvent
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class PaymentCreatedEvent {
    private final PaymentId paymentId;
    private final OrderRef orderRef;
    private final Money amount;
    private final Instant createAt;

    public PaymentCreatedEvent(PaymentId paymentId, OrderRef orderRef, Money amount, Instant createAt) {
        this.paymentId = paymentId;
        this.orderRef = orderRef;
        this.amount = amount;
        this.createAt = createAt;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public OrderRef getOrderRef() { return orderRef; }
    public Money getAmount() { return amount; }
    public Instant getCreateAt() { return createAt; }
}