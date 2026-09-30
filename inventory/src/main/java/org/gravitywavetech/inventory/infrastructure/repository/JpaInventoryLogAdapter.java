package org.gravitywavetech.inventory.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.gravitywavetech.extended.jpa.util.SnowflakeUtil;
import org.gravitywavetech.inventory.application.port.InventoryLogPort;
import org.gravitywavetech.inventory.domain.event.StockChangedEvent;
import org.gravitywavetech.inventory.infrastructure.repository.jpa.InventoryLogJpaEntity;
import org.gravitywavetech.inventory.infrastructure.repository.jpa.InventoryLogJpaRepository;
import org.springframework.stereotype.Component;

/**
 * {@link InventoryLogPort} 的 JPA 实现：把 {@link StockChangedEvent}
 * 落为 {@code t_inventory_log} 一行审计记录。
 *
 * <p>必须在事务上下文内调用（由应用服务的 {@code @Transactional} 保证），
 * 与库存变更原子提交。</p>
 */
@Component
@RequiredArgsConstructor
public class JpaInventoryLogAdapter implements InventoryLogPort {

    private final InventoryLogJpaRepository inventoryLogJpaRepository;

    @Override
    public void append(StockChangedEvent event, Long orderId) {
        InventoryLogJpaEntity logEntry = new InventoryLogJpaEntity();
        logEntry.setId(SnowflakeUtil.nextId());
        logEntry.setInventoryId(event.inventoryId().getId());
        logEntry.setProductId(event.productId());
        logEntry.setProductName(event.productName());
        logEntry.setOrderId(orderId);
        logEntry.setOperation(event.changeType());
        logEntry.setQuantity(event.changeAmount());
        logEntry.setStockAfter(event.remaining());
        logEntry.setOccurredAt(event.occurredAt());
        inventoryLogJpaRepository.save(logEntry);
    }
}
