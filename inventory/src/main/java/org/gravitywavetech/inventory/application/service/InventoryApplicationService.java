package org.gravitywavetech.inventory.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.extended.jpa.util.SnowflakeUtil;
import org.gravitywavetech.inventory.application.command.CreateStockCommand;
import org.gravitywavetech.inventory.application.command.DeductStockCommand;
import org.gravitywavetech.inventory.application.command.ReplenishStockCommand;
import org.gravitywavetech.inventory.application.dto.StockResponse;
import org.gravitywavetech.inventory.domain.event.StockChangedEvent;
import org.gravitywavetech.inventory.domain.exception.ProductNotFoundException;
import org.gravitywavetech.inventory.domain.model.Inventory;
import org.gravitywavetech.inventory.domain.model.InventoryId;
import org.gravitywavetech.inventory.domain.repository.InventoryRepository;
import org.gravitywavetech.inventory.infrastructure.repository.jpa.InventoryLogJpaEntity;
import org.gravitywavetech.inventory.infrastructure.repository.jpa.InventoryLogJpaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 库存应用服务。
 *
 * <p>负责编排「初始化 / 补货 / 扣减 / 查询」四类业务用例，
 * 保持 Inventory 聚合的领域规则在 Inventory 内部，本服务只做流程调度 + 事件发布 + 审计日志。</p>
 *
 * <p>事务边界：所有写操作都加 {@code @Transactional}，
 * 保证「修改聚合 + 持久化 + 写审计日志 + 发布领域事件」四者原子。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryApplicationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLogJpaRepository inventoryLogJpaRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 初始化库存记录（管理员或系统启动时调用）。
     *
     * <p>幂等：同一 productId 重复调用会抛出 {@link IllegalArgumentException}，
     * 调用方应先 {@link #getStock(Long)} 检查。</p>
     */
    @Transactional
    public StockResponse createStock(CreateStockCommand cmd) {
        if (inventoryRepository.findByProductId(cmd.productId()).isPresent()) {
            throw new IllegalArgumentException("商品库存记录已存在，productId=" + cmd.productId());
        }
        InventoryId inventoryId = new InventoryId(SnowflakeUtil.nextId());
        Inventory inventory = Inventory.create(inventoryId, cmd.productId(), cmd.productName(), cmd.initialStock());
        Inventory saved = inventoryRepository.save(inventory);

        // 注意：save() 返回的聚合是通过 rehydrate 重建的（不含 domain events），
        // 事件只挂在原始 `inventory` 实例上，所以要从 inventory 取事件。
        persistAuditLog(inventory);
        inventory.getDomainEvents().forEach(eventPublisher::publishEvent);
        inventory.clearDomainEvents();

        log.info("库存初始化完成，productId={}, initialStock={}", cmd.productId(), cmd.initialStock());
        return StockResponse.from(saved);
    }

    /**
     * 补货。
     */
    @Transactional
    public StockResponse replenishStock(ReplenishStockCommand cmd) {
        Inventory inventory = inventoryRepository.findByProductId(cmd.productId())
                .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));
        inventory.replenish(cmd.quantity());
        Inventory saved = inventoryRepository.save(inventory);

        persistAuditLog(inventory);
        inventory.getDomainEvents().forEach(eventPublisher::publishEvent);
        inventory.clearDomainEvents();

        log.info("补货完成，productId={}, quantity={}, available={}",
                cmd.productId(), cmd.quantity(), saved.getAvailableStock());
        return StockResponse.from(saved);
    }

    /**
     * 扣减库存（已支付订单触发）。
     *
     * <p>由 {@code InventoryDeductConsumer} 调用，
     * 库存不足时抛出 {@link org.gravitywavetech.inventory.domain.exception.InsufficientStockException}，
     * 事务回滚。</p>
     */
    @Transactional
    public StockResponse deductStock(DeductStockCommand cmd) {
        return deductStock(cmd.productId(), cmd.quantity(), cmd.orderId());
    }

    /**
     * 扣减库存（按商品 ID + 数量 + 关联订单）。
     */
    @Transactional
    public StockResponse deductStock(Long productId, int quantity, Long orderId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        inventory.deduct(quantity);
        Inventory saved = inventoryRepository.save(inventory);

        persistAuditLog(inventory, orderId);
        inventory.getDomainEvents().forEach(eventPublisher::publishEvent);
        inventory.clearDomainEvents();

        log.info("扣减库存完成，productId={}, quantity={}, orderId={}, available={}",
                productId, quantity, orderId, saved.getAvailableStock());
        return StockResponse.from(saved);
    }

    /**
     * 查询单个商品的库存。
     */
    @Transactional(readOnly = true)
    public StockResponse getStock(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return StockResponse.from(inventory);
    }

    /**
     * 列出全部库存（demo 用，生产请加分页）。
     */
    @Transactional(readOnly = true)
    public List<StockResponse> listStock() {
        return inventoryRepository.findAll().stream()
                .map(StockResponse::from)
                .toList();
    }

    /**
     * 遍历聚合上暂存的领域事件，为每个 {@link StockChangedEvent} 写一条审计日志。
     * 未关联订单的场景（初始化 / 补货）orderId 为 null。
     */
    private void persistAuditLog(Inventory inventory) {
        persistAuditLog(inventory, null);
    }

    private void persistAuditLog(Inventory inventory, Long orderId) {
        for (Object evt : inventory.getDomainEvents()) {
            if (evt instanceof StockChangedEvent sc) {
                InventoryLogJpaEntity logEntry = new InventoryLogJpaEntity();
                logEntry.setId(SnowflakeUtil.nextId());
                logEntry.setInventoryId(sc.inventoryId().getId());
                logEntry.setProductId(sc.productId());
                logEntry.setProductName(sc.productName());
                logEntry.setOrderId(orderId);
                logEntry.setOperation(sc.changeType());
                logEntry.setQuantity(sc.changeAmount());
                logEntry.setStockAfter(sc.remaining());
                logEntry.setOccurredAt(sc.occurredAt());
                inventoryLogJpaRepository.save(logEntry);
            }
        }
    }
}
