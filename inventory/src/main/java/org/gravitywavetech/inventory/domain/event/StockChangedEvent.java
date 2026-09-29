package org.gravitywavetech.inventory.domain.event;

import org.gravitywavetech.inventory.domain.model.InventoryId;

import java.time.Instant;

/**
 * 库存变更领域事件。
 *
 * <p>当 {@code Inventory} 聚合完成 deduct / replenish 等领域行为时发布，
 * 用于驱动下游（通知、统计、报表等）的副作用。</p>
 *
 * @param inventoryId 库存主键
 * @param productId   商品 ID
 * @param productName 商品名称
 * @param changeType  变更类型（DEDUCT / REPLENISH）
 * @param changeAmount 变更数量
 * @param remaining   变更后可售库存
 * @param occurredAt  发生时间
 */
public record StockChangedEvent(
        InventoryId inventoryId,
        Long productId,
        String productName,
        String changeType,
        int changeAmount,
        int remaining,
        Instant occurredAt
) {
}
