package org.gravitywavetech.order.application.dto;

import org.gravitywavetech.order.domain.model.Order;
import org.gravitywavetech.order.domain.model.OrderStatus;

import java.math.BigDecimal;

/**
 * 订单响应。
 *
 * @param id            订单 ID
 * @param buyerId       买家 ID
 * @param status        订单状态
 * @param totalAmount   总金额
 * @param province      收货省
 * @param city          收货市
 * @param detailAddress 收货详细地址
 */
public record OrderResponse(
        Long id,
        Long buyerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String province,
        String city,
        String detailAddress
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId().getId(),
                order.getBuyerId(),
                order.getStatus(),
                order.getTotalAmount().getAmount(),
                order.getShippingAddress().getProvince(),
                order.getShippingAddress().getCity(),
                order.getShippingAddress().getDetail()
        );
    }
}
