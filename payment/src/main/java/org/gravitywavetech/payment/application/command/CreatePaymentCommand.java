package org.gravitywavetech.payment.application.command;

import org.gravitywavetech.payment.domain.model.Money;
import org.gravitywavetech.payment.domain.model.OrderRef;
import org.gravitywavetech.payment.domain.model.PaymentId;
import org.gravitywavetech.payment.domain.model.PaymentMethod;

/**
 * CreatePaymentCommand
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */

public record CreatePaymentCommand(PaymentId paymentId,
                                   OrderRef orderRef,
                                   Money amount,
                                   PaymentMethod paymentMethod) {
}

