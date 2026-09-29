package org.gravitywavetech.inventory.application.command;

/**
 * 扣减库存命令。
 *
 * @param productId 商品 ID
 * @param quantity  扣减数量（> 0）
 * @param orderId   关联订单 ID（用于追溯与幂等）
 */
public record DeductStockCommand(
        Long productId,
        int quantity,
        Long orderId
) {
}
