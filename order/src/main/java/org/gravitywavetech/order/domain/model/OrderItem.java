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
import java.math.BigDecimal;

@Getter
public class OrderItem {
    private final Long productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;

    public OrderItem(Long productId, String productName, int quantity, Money unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Money getSubTotal(){
        BigDecimal sub = unitPrice.getAmount().multiply(BigDecimal.valueOf(quantity));
        return Money.of(sub);
    }
}
