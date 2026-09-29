package org.gravitywavetech.inventory.domain.exception;

/**
 * 商品库存记录不存在异常。
 */
public class ProductNotFoundException extends RuntimeException {

    private final Long productId;

    public ProductNotFoundException(Long productId) {
        super("商品库存记录不存在：productId=" + productId);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
