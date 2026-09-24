package org.gravitywavetech.order.domain.event;

import org.gravitywavetech.order.domain.model.OrderId;

/**
 * OrderCancelledWithRefundEvent
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class OrderCancelledWithRefundEvent {
    private final OrderId orderId;
    public OrderCancelledWithRefundEvent(OrderId orderId) {
        this.orderId = orderId;
    }
    public OrderId getOrderId() {
        return orderId;
    }
}

