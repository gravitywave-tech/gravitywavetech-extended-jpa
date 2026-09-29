package org.gravitywavetech.inventory.domain.model;

import org.gravitywavetech.inventory.domain.event.StockChangedEvent;
import org.gravitywavetech.inventory.domain.exception.InsufficientStockException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 库存聚合根。
 *
 * <p>每个商品对应一条 {@code Inventory} 记录，由 {@code productId} 唯一标识。
 * 聚合内部维护 {@code totalStock / lockedStock / availableStock} 三个字段，
 * 保证不变式：{@code availableStock = totalStock - lockedStock >= 0}。</p>
 *
 * <p>外部只能通过工厂方法 {@link #create} 与 {@link #rehydrate} 构造，
 * 领域行为 {@link #deduct}、{@link #replenish}、{@link #lock}、{@link #releaseLock}
 * 负责状态流转并暂存领域事件，由应用服务统一发布。</p>
 */
public class Inventory {

    private InventoryId id;
    private Long productId;
    private String productName;
    private int totalStock;
    private int lockedStock;

    private final List<Object> domainEvents = new ArrayList<>();

    private Inventory() {}

    /**
     * 工厂方法：新建库存记录（仅用于初始化商品的初始库存）。
     */
    public static Inventory create(InventoryId id, Long productId, String productName, int totalStock) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("productId 非法");
        }
        if (totalStock < 0) {
            throw new IllegalArgumentException("初始库存不能为负");
        }
        Inventory inventory = new Inventory();
        inventory.id = id;
        inventory.productId = productId;
        inventory.productName = productName;
        inventory.totalStock = totalStock;
        inventory.lockedStock = 0;
        inventory.domainEvents.add(new StockChangedEvent(
                id, productId, productName, "CREATE", totalStock, availableOf(inventory), Instant.now()));
        return inventory;
    }

    /**
     * 从持久化状态重建聚合（不触发领域事件）。
     */
    public static Inventory rehydrate(InventoryId id, Long productId, String productName,
                                      int totalStock, int lockedStock) {
        Inventory inventory = new Inventory();
        inventory.id = id;
        inventory.productId = productId;
        inventory.productName = productName;
        inventory.totalStock = totalStock;
        inventory.lockedStock = lockedStock;
        return inventory;
    }

    /**
     * 领域行为：扣减库存（已支付订单触发）。
     *
     * <p>从可售库存中直接扣减（不经过锁定阶段，简化流程）。
     * 库存不足时抛 {@link InsufficientStockException}，由应用服务回滚事务。</p>
     */
    public void deduct(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("扣减数量必须大于 0");
        }
        int available = availableOf(this);
        if (available < quantity) {
            throw new InsufficientStockException(productId, available, quantity);
        }
        this.totalStock -= quantity;
        if (this.totalStock < this.lockedStock) {
            this.totalStock = this.lockedStock;
        }
        this.domainEvents.add(new StockChangedEvent(
                id, productId, productName, "DEDUCT", quantity, availableOf(this), Instant.now()));
    }

    /**
     * 领域行为：补货（管理员操作或供应商到货）。
     */
    public void replenish(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("补货数量必须大于 0");
        }
        int before = availableOf(this);
        this.totalStock += quantity;
        this.domainEvents.add(new StockChangedEvent(
                id, productId, productName, "REPLENISH", quantity, availableOf(this), Instant.now()));
    }

    /**
     * 领域行为：锁定库存（下单但未支付）。
     */
    public void lock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("锁定数量必须大于 0");
        }
        int available = availableOf(this);
        if (available < quantity) {
            throw new InsufficientStockException(productId, available, quantity);
        }
        this.lockedStock += quantity;
        this.domainEvents.add(new StockChangedEvent(
                id, productId, productName, "LOCK", quantity, availableOf(this), Instant.now()));
    }

    /**
     * 领域行为：释放锁定（订单取消）。
     */
    public void releaseLock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("释放数量必须大于 0");
        }
        if (this.lockedStock < quantity) {
            throw new IllegalStateException(String.format(
                    "释放数量 %d 超过已锁定数量 %d", quantity, this.lockedStock));
        }
        this.lockedStock -= quantity;
        this.domainEvents.add(new StockChangedEvent(
                id, productId, productName, "RELEASE", quantity, availableOf(this), Instant.now()));
    }

    public void rename(String productName) {
        this.productName = productName;
    }

    public StockStatus getStatus() {
        return availableOf(this) > 0 ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK;
    }

    private static int availableOf(Inventory inv) {
        return Math.max(0, inv.totalStock - inv.lockedStock);
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public InventoryId getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public int getLockedStock() {
        return lockedStock;
    }

    public int getAvailableStock() {
        return availableOf(this);
    }
}
