package org.gravitywavetech.order.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.domain.model.OrderId;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 库存服务客户端（防腐层）。
 *
 * <p>通过 Spring Cloud Stream 向 inventory-service 发送「扣减库存」消息，
 * 由 inventory 服务的 {@code InventoryDeductConsumer} 异步消费。
 * 订单服务不直接调用库存服务 HTTP，避免跨服务强耦合。</p>
 *
 * <p>消息契约见 {@link InventoryDeductMessage}：
 * 当前仅传 {@code orderId}，items 暂为空列表；
 * 后续可在 {@code OrderPaidListener} 处补齐商品明细后扩展。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private static final String OUTPUT = "inventoryDeduct-out-0";

    private final StreamBridge streamBridge;

    public void deduct(OrderId orderId) {
        InventoryDeductMessage payload = new InventoryDeductMessage(
                orderId.getId(),
                List.of(),
                Instant.now()
        );
        String traceId = UUID.randomUUID().toString();
        Message<InventoryDeductMessage> outbound = MessageBuilder
                .withPayload(payload)
                .setHeader("traceId", traceId)
                .build();
        log.info("发送扣减库存请求，traceId={}, orderId={}", traceId, orderId.getId());
        streamBridge.send(OUTPUT, outbound);
    }
}
