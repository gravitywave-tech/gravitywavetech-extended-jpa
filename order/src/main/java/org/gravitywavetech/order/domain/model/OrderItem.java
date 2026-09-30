package org.gravitywavetech.order.domain.model;

/**
 * OrderItem
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
import lombok.Getter;

@Getter
public class OrderItem {
    private final Long productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;

    public OrderItem(Long productId, String productName, int quantity, Money unitPrice) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("订单项 productId 非法");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("订单项数量必须大于 0，productId=" + productId);
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("订单项单价不能为空，productId=" + productId);
        }
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /** 小计 = 单价 × 数量，算术封闭在 Money 值对象内。 */
    public Money getSubTotal() {
        return unitPrice.multiply(quantity);
    }
}
