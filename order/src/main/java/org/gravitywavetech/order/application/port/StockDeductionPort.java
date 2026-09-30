package org.gravitywavetech.order.application.port;

import org.gravitywavetech.order.domain.model.OrderId;
import org.gravitywavetech.order.domain.model.OrderItem;

import java.util.List;

/**
 * 「扣减库存」出站端口。
 *
 * <p>订单支付成功后需扣减库存，但库存属于 inventory 上下文，
 * order 只发出「哪些商品扣多少」的意图，由对方决定如何执行。
 * 应用层依赖本接口而非具体的 MQ 客户端，隔离跨上下文技术细节。</p>
 */
public interface StockDeductionPort {

    /**
     * 请求 inventory 服务按订单明细扣减库存。
     *
     * @param orderId 订单 ID
     * @param items   订单明细（productId + quantity）
     */
    void deductForOrder(OrderId orderId, List<OrderItem> items);
}
