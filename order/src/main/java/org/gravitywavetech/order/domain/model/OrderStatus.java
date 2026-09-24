package org.gravitywavetech.order.domain.model;

/**
 * OrderStatus
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public enum OrderStatus {
    WAITING_PAYMENT,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELLED
}