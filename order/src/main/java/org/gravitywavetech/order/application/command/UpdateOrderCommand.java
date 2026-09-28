package org.gravitywavetech.order.application.command;

/**
 * 修改订单命令。
 *
 * <p>当前仅支持修改收货地址；订单其他字段（金额、状态）由领域行为驱动，不允许外部直接改。</p>
 */
public record UpdateOrderCommand(
        String addressProvince,
        String addressCity,
        String addressDetail
) {
}
