package org.gravitywavetech.inventory.application.command;

/**
 * 补货命令。
 *
 * @param productId 商品 ID
 * @param quantity  补货数量（> 0）
 */
public record ReplenishStockCommand(
        Long productId,
        int quantity
) {
}
