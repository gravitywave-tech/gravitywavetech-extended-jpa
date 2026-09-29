package org.gravitywavetech.inventory.domain.model;

import lombok.EqualsAndHashCode;

/**
 * 库存主键值对象，包装 Long。
 */
@EqualsAndHashCode
public class InventoryId {
    private final Long id;

    public InventoryId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("库存 ID 非法");
        }
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
