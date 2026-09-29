package org.gravitywavetech.inventory.domain.repository;

import org.gravitywavetech.inventory.domain.exception.ProductNotFoundException;
import org.gravitywavetech.inventory.domain.model.Inventory;
import org.gravitywavetech.inventory.domain.model.InventoryId;

import java.util.List;
import java.util.Optional;

/**
 * 库存仓储接口（DDD 端口）。
 *
 * <p>由 {@code infrastructure.repository.SpringDataInventoryRepository} 实现，
 * 隔离领域层与 Spring Data JPA。</p>
 */
public interface InventoryRepository {

    /** 按主键查找。 */
    Inventory findById(InventoryId inventoryId);

    /** 按商品 ID 查找（业务主键）。 */
    Optional<Inventory> findByProductId(Long productId);

    /** 保存（新增或更新）。 */
    Inventory save(Inventory inventory);

    /** 列出全部库存。 */
    List<Inventory> findAll();
}
