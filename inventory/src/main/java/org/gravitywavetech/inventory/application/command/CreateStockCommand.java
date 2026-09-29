package org.gravitywavetech.inventory.application.command;

/**
 * 初始化库存命令。
 *
 * @param productId   商品 ID
 * @param productName 商品名称
 * @param initialStock 初始库存数量
 */
public record CreateStockCommand(
        Long productId,
        String productName,
        int initialStock
) {
}
