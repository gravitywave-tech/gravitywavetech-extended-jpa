package org.gravitywavetech.order.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.domain.model.OrderId;
import org.springframework.stereotype.Component;

/**
 * InventoryClient
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@Slf4j
@Component
public class InventoryClient {
    public void deduct(OrderId orderId){
        // 远程调用库存服务扣减库存
        log.info("扣减库存，orderId={}", orderId.getId());
    }
}
