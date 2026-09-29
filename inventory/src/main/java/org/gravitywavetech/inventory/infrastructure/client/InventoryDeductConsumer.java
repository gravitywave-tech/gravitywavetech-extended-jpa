package org.gravitywavetech.inventory.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.inventory.application.service.InventoryApplicationService;
import org.gravitywavetech.inventory.domain.exception.InsufficientStockException;
import org.gravitywavetech.inventory.domain.exception.ProductNotFoundException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * 消费 order 服务发出的扣减库存消息。
 *
 * <p>Bean 名称 {@code inventoryDeduct} 必须等于 binding 前缀
 * （{@code inventoryDeduct-in-0}），通过
 * {@code spring.cloud.function.definition=inventoryDeduct} 声明。</p>
 *
 * <p>异常处理策略：
 * <ul>
 *   <li>{@link ProductNotFoundException} — 商品不存在，记录 error 并跳过该条（不重试）</li>
 *   <li>{@link InsufficientStockException} — 库存不足，记录 error 并抛出，
 *       让 Spring Cloud Stream 触发重试或 DLQ</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component("inventoryDeduct")
@RequiredArgsConstructor
public class InventoryDeductConsumer implements Consumer<Message<InventoryDeductMessage>> {

    private final InventoryApplicationService inventoryApplicationService;

    @Override
    public void accept(Message<InventoryDeductMessage> message) {
        InventoryDeductMessage payload = message.getPayload();
        log.info("消费扣减库存消息，orderId={}, items={}, traceId={}",
                payload.orderId(), payload.items(), message.getHeaders().get("traceId"));

        if (payload.items() == null || payload.items().isEmpty()) {
            log.warn("扣减库存消息 items 为空，跳过实际扣减（订单侧尚未传递商品明细），orderId={}", payload.orderId());
            return;
        }

        for (InventoryDeductMessage.ItemDeduct item : payload.items()) {
            log.info("扣减商品库存，productId={}, quantity={}", item.productId(), item.quantity());
            inventoryApplicationService.deductStock(item.productId(), item.quantity(), payload.orderId());
        }
        log.info("扣减库存完成，orderId={}", payload.orderId());
    }
}
