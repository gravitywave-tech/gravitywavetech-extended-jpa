package org.gravitywavetech.order.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.application.port.StockDeductionPort;
import org.gravitywavetech.order.domain.model.OrderId;
import org.gravitywavetech.order.domain.model.OrderItem;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@link StockDeductionPort} 的 RabbitMQ 实现（防腐层）。
 *
 * <p>通过 Spring Cloud Stream 向 inventory 服务发送「扣减库存」消息，
 * 由 inventory 服务的 {@code InventoryDeductConsumer} 异步消费。
 * 订单服务不直接调用库存服务 HTTP，避免跨服务强耦合。</p>
 *
 * <p>消息契约见 {@link InventoryDeductMessage}：
 * 携带 {@code orderId} 与 {@code items}（productId + quantity），
 * 让 inventory 服务无需反向调用 order 即可完整执行扣减。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient implements StockDeductionPort {

    private static final String OUTPUT = "inventoryDeduct-out-0";

    private final StreamBridge streamBridge;

    @Override
    public void deductForOrder(OrderId orderId, List<OrderItem> items) {
        List<InventoryDeductMessage.ItemDeduct> itemDeducts = items == null
                ? List.of()
                : items.stream()
                        .map(item -> new InventoryDeductMessage.ItemDeduct(
                                item.getProductId(), item.getQuantity()))
                        .toList();

        InventoryDeductMessage payload = new InventoryDeductMessage(
                orderId.getId(),
                itemDeducts,
                Instant.now()
        );
        String traceId = UUID.randomUUID().toString();
        Message<InventoryDeductMessage> outbound = MessageBuilder
                .withPayload(payload)
                .setHeader("traceId", traceId)
                .build();
        log.info("发送扣减库存请求，traceId={}, orderId={}, items={}",
                traceId, orderId.getId(), itemDeducts);
        streamBridge.send(OUTPUT, outbound);
    }
}
