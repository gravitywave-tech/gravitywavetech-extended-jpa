package org.gravitywavetech.order.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.application.port.NotificationPort;
import org.gravitywavetech.order.domain.model.OrderId;
import org.springframework.stereotype.Component;

/**
 * {@link NotificationPort} 的本地实现：仅记录日志。
 *
 * <p>demo 中不接真实短信通道；生产实现可替换为短信网关 / 站内信客户端，
 * 应用层代码不变。</p>
 */
@Slf4j
@Component
public class NotificationClient implements NotificationPort {

    @Override
    public void notifyOrderPaid(OrderId orderId) {
        log.info("发送支付成功短信给用户，orderId={}", orderId.getId());
    }
}
