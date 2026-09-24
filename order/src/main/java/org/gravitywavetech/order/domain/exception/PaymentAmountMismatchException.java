package org.gravitywavetech.order.domain.exception;

/**
 * PaymentAmountMismatchException
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class PaymentAmountMismatchException extends RuntimeException{
    public PaymentAmountMismatchException(){
        super("支付金额与订单金额不一致");
    }
}