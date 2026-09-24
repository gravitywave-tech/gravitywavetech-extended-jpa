package org.gravitywavetech.order.infrastructure.repository;

/**
 * SpringDataOrderRepository
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
import org.gravitywavetech.order.domain.exception.OrderNotFoundException;
import org.gravitywavetech.order.domain.model.Address;
import org.gravitywavetech.order.domain.model.Order;
import org.gravitywavetech.order.domain.model.OrderId;
import org.gravitywavetech.order.domain.repository.OrderRepository;
import org.gravitywavetech.order.infrastructure.repository.jpa.OrderJpaEntity;
import org.gravitywavetech.order.infrastructure.repository.jpa.OrderJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
        // 【转换器】jpaEntity -> Order聚合根，实际项目抽单独Mapper类
        Address address = Address.of(jpaEntity.getProvince(), jpaEntity.getCity(), jpaEntity.getDetailAddress());
        // 这里简化演示，实际要加载OrderItem列表
        return Order.create(
                new OrderId(jpaEntity.getId()),
                jpaEntity.getBuyerId(),
                List.of(),
                address
        );
    }

    @Override
    public void save(Order order) {
        OrderJpaEntity jpaEntity = new OrderJpaEntity();
        jpaEntity.setId(order.getId().getId());
        jpaEntity.setBuyerId(order.getBuyerId());
        jpaEntity.setStatus(order.getStatus());
        jpaEntity.setTotalAmount(order.getTotalAmount().getAmount());
        jpaEntity.setProvince(order.getShippingAddress().getProvince());
        jpaEntity.setCity(order.getShippingAddress().getCity());
        jpaEntity.setDetailAddress(order.getShippingAddress().getDetail());
        jpaRepository.save(jpaEntity);
    }
}
