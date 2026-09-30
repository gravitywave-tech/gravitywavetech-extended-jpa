package org.gravitywavetech.inventory.infrastructure.repository.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

/**
 * 库存变更审计日志 JPA 实体，映射 {@code t_inventory_log} 表。
 *
 * <p>每次 {@code Inventory} 聚合发生 {@code DEDUCT / REPLENISH / LOCK / RELEASE}
 * 等领域行为时写入一条日志，通过 {@code order_id} 与订单服务建立跨服务追踪关系，
 * 便于事后审计与排查。</p>
 *
 * <p>主键由 {@code SnowflakeUtil} 生成，与 {@link InventoryJpaEntity} 保持一致。</p>
 */
@Entity
@Table(
        name = "t_inventory_log",
        indexes = {
                @Index(name = "idx_inventory_log_inventory_id", columnList = "inventory_id"),
                @Index(name = "idx_inventory_log_product_id", columnList = "product_id"),
                @Index(name = "idx_inventory_log_order_id", columnList = "order_id"),
                @Index(name = "idx_inventory_log_occurred_at", columnList = "occurred_at")
        }
)
@Data
public class InventoryLogJpaEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "operation", length = 20, nullable = false)
    private String operation;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
