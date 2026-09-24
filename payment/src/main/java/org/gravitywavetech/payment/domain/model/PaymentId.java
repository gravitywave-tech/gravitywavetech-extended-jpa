package org.gravitywavetech.payment.domain.model;

import lombok.EqualsAndHashCode;

/**
 * PaymentId
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@EqualsAndHashCode
public class PaymentId {
    private final Long id;
    public PaymentId(Long id) {
        if(id == null || id <=0){
            throw new IllegalArgumentException("支付ID非法");
        }
        this.id = id;
    }
    public Long getId() {
        return id;
    }
}
