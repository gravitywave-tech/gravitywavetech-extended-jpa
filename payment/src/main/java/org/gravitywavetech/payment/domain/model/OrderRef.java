package org.gravitywavetech.payment.domain.model;

import lombok.EqualsAndHashCode;

/**
 * OrderRef
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@EqualsAndHashCode
public class OrderRef {
    private final Long orderId;
    public OrderRef(Long orderId) {
        if(orderId == null || orderId <=0){
            throw new IllegalArgumentException("订单引用ID非法");
        }
        this.orderId = orderId;
    }
    public Long getOrderId() {
        return orderId;
    }
}
