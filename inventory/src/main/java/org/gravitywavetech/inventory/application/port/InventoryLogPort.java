package org.gravitywavetech.inventory.application.port;

import org.gravitywavetech.inventory.domain.event.StockChangedEvent;

/**
 * 库存审计日志出站端口。
 *
 * <p>库存变更审计属于应用层副作用而非 Inventory 聚合的领域知识，
 * 应用层通过本端口表达「追加一条审计日志」的意图，
 * 由 infrastructure 层决定落库方式（当前为 JPA 写 {@code t_inventory_log}）。</p>
 */
public interface InventoryLogPort {

    /**
     * 在与业务变更同一事务内追加一条库存审计日志。
     *
     * @param event   触发日志的库存变更事件（携带变更前后数据）
     * @param orderId 关联订单 ID，无订单关联的场景（初始化 / 补货）传 null
     */
    void append(StockChangedEvent event, Long orderId);
}
