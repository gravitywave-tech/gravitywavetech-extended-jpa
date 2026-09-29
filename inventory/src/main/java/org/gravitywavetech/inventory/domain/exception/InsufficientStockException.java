package org.gravitywavetech.inventory.domain.exception;

/**
 * 库存不足异常。
 *
 * <p>当扣减数量超过可售库存时抛出。</p>
 */
public class InsufficientStockException extends RuntimeException {

    private final Long productId;
    private final int available;
    private final int requested;

    public InsufficientStockException(Long productId, int available, int requested) {
        super(String.format("库存不足：商品 %d 可售库存 %d，请求扣减 %d", productId, available, requested));
        this.productId = productId;
        this.available = available;
        this.requested = requested;
    }

    public Long getProductId() {
        return productId;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
