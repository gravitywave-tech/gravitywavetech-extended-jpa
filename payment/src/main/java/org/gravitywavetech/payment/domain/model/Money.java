package org.gravitywavetech.payment.domain.model;

import java.math.BigDecimal;

/**
 * Money
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class Money {
    private final BigDecimal amount;
    public Money(BigDecimal amount) {
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <0){
            throw new IllegalArgumentException("支付金额不能为负数");
        }
        this.amount = amount;
    }
    public static Money of(BigDecimal val){
        return new Money(val);
    }
    public BigDecimal getAmount() {
        return amount;
    }
}