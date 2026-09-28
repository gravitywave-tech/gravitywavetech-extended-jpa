package org.gravitywavetech.order.infrastructure.repository;

import org.gravitywavetech.order.domain.exception.OrderNotFoundException;
import org.gravitywavetech.order.domain.model.Address;
import org.gravitywavetech.order.domain.model.Money;
import org.gravitywavetech.order.domain.model.Order;
import org.gravitywavetech.order.domain.model.OrderId;
import org.gravitywavetech.order.domain.repository.OrderRepository;
import org.gravitywavetech.order.infrastructure.repository.jpa.OrderJpaEntity;
import org.gravitywavetech.order.infrastructure.repository.jpa.OrderJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SpringDataOrderRepository
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@Repository
public class SpringDataOrderRepository implements OrderRepository {
    private final OrderJpaRepository jpaRepository;

    public SpringDataOrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order findById(OrderId orderId) {
        OrderJpaEntity jpaEntity = jpaRepository.findById(orderId.getId())
                .orElseThrow(OrderNotFoundException::new);
        return toDomain(jpaEntity);
    }

    @Override
    public void save(Order order) {
        OrderJpaEntity jpaEntity = toJpaEntity(order);
        jpaRepository.save(jpaEntity);
    }

    @Override
    public void delete(Order order) {
        jpaRepository.delete(toJpaEntity(order));
    }

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private Order toDomain(OrderJpaEntity jpaEntity) {
        Address address = Address.of(jpaEntity.getProvince(), jpaEntity.getCity(), jpaEntity.getDetailAddress());
        // 用 rehydrate 而非 create，避免把 status 重置为 WAITING_PAYMENT、也避免重复发 OrderCreatedEvent
        return Order.rehydrate(
                new OrderId(jpaEntity.getId()),
                jpaEntity.getBuyerId(),
                List.of(),
                address,
                jpaEntity.getStatus(),
                Money.of(jpaEntity.getTotalAmount())
        );
    }

    private OrderJpaEntity toJpaEntity(Order order) {
        OrderJpaEntity jpaEntity = new OrderJpaEntity();
        jpaEntity.setId(order.getId().getId());
        jpaEntity.setBuyerId(order.getBuyerId());
        jpaEntity.setStatus(order.getStatus());
        jpaEntity.setTotalAmount(order.getTotalAmount().getAmount());
        jpaEntity.setProvince(order.getShippingAddress().getProvince());
        jpaEntity.setCity(order.getShippingAddress().getCity());
        jpaEntity.setDetailAddress(order.getShippingAddress().getDetail());
        return jpaEntity;
    }
}
