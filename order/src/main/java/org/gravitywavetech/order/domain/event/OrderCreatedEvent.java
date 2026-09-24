package org.gravitywavetech.order.domain.event;

import org.gravitywavetech.order.domain.model.OrderId;

/**
 * OrderCreatedEvent
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class OrderCreatedEvent {
    private final OrderId orderId;
    public OrderCreatedEvent(OrderId orderId) {
        this.orderId = orderId;
    }
    public OrderId getOrderId() {
        return orderId;
    }
}