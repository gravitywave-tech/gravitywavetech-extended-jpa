package org.gravitywavetech.order.domain.repository;

import org.gravitywavetech.order.domain.exception.OrderNotFoundException;
import org.gravitywavetech.order.domain.model.Order;
import org.gravitywavetech.order.domain.model.OrderId;

/**
 * OrderRepository
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public interface OrderRepository {
    Order findById(OrderId orderId) throws OrderNotFoundException;
    void save(Order order);
}