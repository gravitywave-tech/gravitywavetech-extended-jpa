package org.gravitywavetech.order.application.command;

import java.math.BigDecimal;

/**
 * 订单项请求。
 *
 * @param productId   商品 ID
 * @param productName 商品名称
 * @param quantity    购买数量（>0）
 * @param unitPrice   单价
 */
public record OrderItemRequest(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {
}
