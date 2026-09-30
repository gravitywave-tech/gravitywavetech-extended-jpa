package org.gravitywavetech.inventory.infrastructure.repository.jpa;

import org.gravitywavetech.extended.jpa.repository.ExtendedBaseRepository;

import java.util.List;

/**
 * 库存审计日志 JPA 仓储接口。
 */
public interface InventoryLogJpaRepository extends ExtendedBaseRepository<InventoryLogJpaEntity, Long> {

    List<InventoryLogJpaEntity> findByProductId(Long productId);

    List<InventoryLogJpaEntity> findByOrderId(Long orderId);

    List<InventoryLogJpaEntity> findByInventoryId(Long inventoryId);
}
