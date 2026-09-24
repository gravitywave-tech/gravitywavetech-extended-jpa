package org.gravitywavetech.payment.domain.event;

import org.gravitywavetech.payment.domain.model.Money;
import org.gravitywavetech.payment.domain.model.OrderRef;
import org.gravitywavetech.payment.domain.model.PaymentId;

import java.time.Instant;

/**
 * PaymentSuccessEvent
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class PaymentSuccessEvent {
    private final PaymentId paymentId;
    private final OrderRef orderRef;
    private final Money paidAmount;
    private final String thirdPartyTradeNo;
    private final Instant paidAt;

    public PaymentSuccessEvent(PaymentId paymentId,
                               OrderRef orderRef,
                               Money paidAmount,
                               String thirdPartyTradeNo,
                               Instant paidAt) {
        this.paymentId = paymentId;
        this.orderRef = orderRef;
        this.paidAmount = paidAmount;
        this.thirdPartyTradeNo = thirdPartyTradeNo;
        this.paidAt = paidAt;
    }

    public PaymentId getPaymentId() { return paymentId; }
    public OrderRef getOrderRef() { return orderRef; }
    public Money getPaidAmount() { return paidAmount; }
    public String getThirdPartyTradeNo() { return thirdPartyTradeNo; }
    public Instant getPaidAt() { return paidAt; }
}
