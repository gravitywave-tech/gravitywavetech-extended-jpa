package org.gravitywavetech.inventory.infrastructure.repository.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 库存 JPA 实体。
 *
 * <p>不使用 {@code @GeneratedValue}：主键由 {@code InventoryApplicationService}
 * 通过 {@code SnowflakeUtil} 生成，与 order 模块保持一致。</p>
 */
@Entity
@Table(
        name = "t_inventory",
        indexes = {@Index(name = "uk_inventory_product_id", columnList = "product_id", unique = true)}
)
@Data
public class InventoryJpaEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "total_stock", nullable = false)
    private Integer totalStock;

    @Column(name = "locked_stock", nullable = false)
    private Integer lockedStock;
}
