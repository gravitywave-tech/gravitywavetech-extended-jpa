package org.gravitywavetech.inventory.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.gravitywavetech.inventory.domain.exception.ProductNotFoundException;
import org.gravitywavetech.inventory.domain.model.Inventory;
import org.gravitywavetech.inventory.domain.model.InventoryId;
import org.gravitywavetech.inventory.domain.repository.InventoryRepository;
import org.gravitywavetech.inventory.infrastructure.repository.jpa.InventoryJpaEntity;
import org.gravitywavetech.inventory.infrastructure.repository.jpa.InventoryJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA 实现：隔离领域层与持久化细节。
 *
 * <p>把 {@link InventoryJpaEntity} ↔ {@link Inventory} 聚合做映射：
 * 读方向用 {@link Inventory#rehydrate} 避免误触发 {@code StockChangedEvent}；
 * 写方向把可售库存字段显式落到 DB，便于后续审计/报表查询。</p>
 */
@Repository
@RequiredArgsConstructor
public class SpringDataInventoryRepository implements InventoryRepository {

    private final InventoryJpaRepository jpaRepository;

    @Override
    public Inventory findById(InventoryId inventoryId) {
        InventoryJpaEntity entity = jpaRepository.findById(inventoryId.getId())
                .orElseThrow(() -> new ProductNotFoundException(inventoryId.getId()));
        return toDomain(entity);
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        return jpaRepository.findByProductId(productId).map(this::toDomain);
    }

    @Override
    public Inventory save(Inventory inventory) {
        InventoryJpaEntity entity = toJpaEntity(inventory);
        InventoryJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Inventory> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private Inventory toDomain(InventoryJpaEntity entity) {
        return Inventory.rehydrate(
                new InventoryId(entity.getId()),
                entity.getProductId(),
                entity.getProductName(),
                entity.getTotalStock() == null ? 0 : entity.getTotalStock(),
                entity.getLockedStock() == null ? 0 : entity.getLockedStock()
        );
    }

    private InventoryJpaEntity toJpaEntity(Inventory inventory) {
        InventoryJpaEntity entity = new InventoryJpaEntity();
        entity.setId(inventory.getId().getId());
        entity.setProductId(inventory.getProductId());
        entity.setProductName(inventory.getProductName());
        entity.setTotalStock(inventory.getTotalStock());
        entity.setLockedStock(inventory.getLockedStock());
        return entity;
    }
}
