package org.gravitywavetech.inventory.domain.model;

/**
 * 库存状态枚举。
 */
public enum StockStatus {
    /** 有货：可售库存 > 0 */
    IN_STOCK,
    /** 缺货：可售库存 = 0 */
    OUT_OF_STOCK,
    /** 锁定：被订单占用，等待支付或释放 */
    LOCKED
}
