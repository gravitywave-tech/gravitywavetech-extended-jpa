package org.gravitywavetech.order.infrastructure.repository.jpa;

import org.gravitywavetech.extended.jpa.repository.ExtendedBaseRepository;

import java.util.List;

/**
 * 订单项 Spring Data JPA 仓储接口。
 *
 * <p>虽然 {@link OrderJpaEntity} 已通过 {@code @OneToMany} 级联管理订单项，
 * 本接口仍保留用于独立查询（如按 productId 反查所有包含该商品的订单）。</p>
 */
public interface OrderItemJpaRepository extends ExtendedBaseRepository<OrderItemJpaEntity, Long> {

    List<OrderItemJpaEntity> findByOrderId(Long orderId);
}
