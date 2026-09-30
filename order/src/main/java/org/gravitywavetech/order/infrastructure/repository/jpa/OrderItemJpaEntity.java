package org.gravitywavetech.order.infrastructure.repository.jpa;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单项 JPA 实体，映射 {@code t_order_item} 表。
 *
 * <p>与 {@link OrderJpaEntity} 通过 {@code order_id} 外键关联。
 * 主键由 {@code SnowflakeUtil} 生成，与订单主键策略保持一致。</p>
 */
@Entity
@Table(
        name = "t_order_item",
        indexes = {
                @Index(name = "idx_order_item_order_id", columnList = "order_id"),
                @Index(name = "idx_order_item_product_id", columnList = "product_id")
        }
)
@Data
public class OrderItemJpaEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private OrderJpaEntity order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 19, scale = 2, nullable = false)
    private BigDecimal unitPrice;
}
