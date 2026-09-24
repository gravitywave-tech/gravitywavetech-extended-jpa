package org.gravitywavetech.order.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.domain.model.OrderId;
import org.springframework.stereotype.Component;

/**
 * NotificationClient
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@Slf4j
@Component
public class NotificationClient {
    public void notifyUser(OrderId orderId){
        log.info("发送支付成功短信给用户，orderId={}", orderId.getId());
    }
}