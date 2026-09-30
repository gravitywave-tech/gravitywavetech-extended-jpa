package org.gravitywavetech.order.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * OrderPaidEvent
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class OrderPaidEvent {
    private final OrderId orderId;
    private final Instant paidAt;
    private final List<OrderItem> items;

    public OrderPaidEvent(OrderId orderId, Instant paidAt) {
        this(orderId, paidAt, Collections.emptyList());
    }

    public OrderPaidEvent(OrderId orderId, Instant paidAt, List<OrderItem> items) {
        this.orderId = orderId;
        this.paidAt = paidAt;
        this.items = items == null ? Collections.emptyList() : List.copyOf(items);
    }

    public OrderId getOrderId() {
        return orderId;
    }
    public Instant getPaidAt() {
        return paidAt;
    }
    public List<OrderItem> getItems() {
        return items;
    }
}
