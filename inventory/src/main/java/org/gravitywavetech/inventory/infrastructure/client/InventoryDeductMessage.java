package org.gravitywavetech.inventory.infrastructure.client;

import java.time.Instant;
import java.util.List;

/**
 * order 服务向 inventory 服务发出的「扣减库存」消息体。
 *
 * <p>跨服务契约以 JSON 序列化，字段保持最小化。
 * {@code items} 字段携带商品明细（productId + quantity），
 * 让 inventory 服务无需反向调用 order 即可完整执行扣减。</p>
 *
 * @param orderId     触发扣减的订单 ID（用于幂等与追溯）
 * @param items       商品明细列表
 * @param requestedAt 请求发起时间
 */
public record InventoryDeductMessage(
        Long orderId,
        List<ItemDeduct> items,
        Instant requestedAt
) {

    /** 单条扣减明细。 */
    public record ItemDeduct(Long productId, int quantity) {}
}
