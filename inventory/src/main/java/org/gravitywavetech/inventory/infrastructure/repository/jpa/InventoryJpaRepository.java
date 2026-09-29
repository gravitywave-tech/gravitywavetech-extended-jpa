package org.gravitywavetech.inventory.infrastructure.repository.jpa;

import org.gravitywavetech.extended.jpa.repository.ExtendedBaseRepository;

import java.util.Optional;

/**
 * 库存 Spring Data JPA 仓储接口。
 *
 * <p>继承框架提供的 {@link ExtendedBaseRepository}，获得 Native/HQL/Criteria 三套查询能力。</p>
 */
public interface InventoryJpaRepository extends ExtendedBaseRepository<InventoryJpaEntity, Long> {

    Optional<InventoryJpaEntity> findByProductId(Long productId);
}
