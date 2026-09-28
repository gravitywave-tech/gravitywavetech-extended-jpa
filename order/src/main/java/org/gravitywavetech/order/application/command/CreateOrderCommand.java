package org.gravitywavetech.order.application.command;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单命令。
 *
 * @param buyerId          买家 ID
 * @param addressProvince  收货省
 * @param addressCity      收货市
 * @param addressDetail    收货详细地址
 * @param items            订单项
 */
public record CreateOrderCommand(
        Long buyerId,
        String addressProvince,
        String addressCity,
        String addressDetail,
        List<OrderItemRequest> items
) {
}
