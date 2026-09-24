package org.gravitywavetech.order.domain.model;

/**
 * OrderPaidEvent
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
import java.time.Instant;

public class OrderPaidEvent {
    private final OrderId orderId;
    private final Instant paidAt;

    public OrderPaidEvent(OrderId orderId, Instant paidAt) {
        this.orderId = orderId;
        this.paidAt = paidAt;
    }

    public OrderId getOrderId() {
        return orderId;
    }
    public Instant getPaidAt() {
        return paidAt;
    }
}
