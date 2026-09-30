package org.gravitywavetech.order.application.port;

import org.gravitywavetech.order.domain.model.Money;
import org.gravitywavetech.order.domain.model.OrderId;

/**
 * 「创建支付单」出站端口。
 *
 * <p>order 上下文需要通知 payment 服务创建支付单，但不应关心
 * 用的是 RabbitMQ、Kafka 还是 HTTP。应用层只依赖本接口，
 * 具体传输方式（含消息契约 {@code PaymentCreateMessage} 的组装）
 * 封装在 infrastructure 层的实现里。</p>
 */
public interface PaymentCreatePort {

    /**
     * 请求 payment 服务为订单创建支付单。
     *
     * @param orderId        订单 ID
     * @param amount         应付金额（取自 Order 聚合的 totalAmount）
     * @param paymentMethod  支付方式（如 ALIPAY / WECHAT_PAY）
     */
    void requestCreatePayment(OrderId orderId, Money amount, String paymentMethod);
}
