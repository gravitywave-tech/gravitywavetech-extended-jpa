package org.gravitywavetech.order.application.port;

import org.gravitywavetech.order.domain.model.OrderId;

/**
 * 「订单支付成功通知」出站端口。
 *
 * <p>通知渠道（短信 / 站内信 / push）是实现细节，应用层只表达
 * 「该订单已支付，需要通知用户」这一意图。</p>
 */
public interface NotificationPort {

    /** 通知买家订单已支付成功。 */
    void notifyOrderPaid(OrderId orderId);
}
