package org.gravitywavetech.inventory.application.dto;

import org.gravitywavetech.inventory.domain.model.Inventory;
import org.gravitywavetech.inventory.domain.model.StockStatus;

/**
 * 库存查询响应 DTO。
 *
 * @param id           库存主键
 * @param productId    商品 ID
 * @param productName  商品名称
 * @param totalStock   总库存
 * @param lockedStock  锁定库存
 * @param availableStock 可售库存
 * @param status       库存状态（IN_STOCK / OUT_OF_STOCK / LOCKED）
 */
public record StockResponse(
        Long id,
        Long productId,
        String productName,
        int totalStock,
        int lockedStock,
        int availableStock,
        StockStatus status
) {

    public static StockResponse from(Inventory inventory) {
        return new StockResponse(
                inventory.getId().getId(),
                inventory.getProductId(),
                inventory.getProductName(),
                inventory.getTotalStock(),
                inventory.getLockedStock(),
                inventory.getAvailableStock(),
                inventory.getStatus()
        );
    }
}
