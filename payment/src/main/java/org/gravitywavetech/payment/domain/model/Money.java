package org.gravitywavetech.payment.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Money 金额值对象。
 *
 * <p>equals 采用 {@code compareTo} 语义：{@code 10.0} 与 {@code 10.00} 视为相等。
 * 金额常跨服务经 JSON 往返传输，BigDecimal 的 scale（小数位数）在序列化后不保证稳定，
 * 若用 {@code BigDecimal.equals}（scale 敏感）比对会出现「数值相同却判不等」的误判。</p>
 *
 * <p>金额算术封闭在本类内（add / subtract / multiply），
 * 调用方不再直接操作裸 {@code BigDecimal}，保证结果仍经过非负校验。</p>
 */
public class Money {
    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("支付金额不能为负数");
        }
        this.amount = amount;
    }

    public static Money of(BigDecimal val) {
        return new Money(val);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "相加金额不能为空");
        return new Money(amount.add(other.amount));
    }

    /** 减法结果为负时抛出 {@link IllegalArgumentException}（金额值对象不允许负数）。 */
    public Money subtract(Money other) {
        Objects.requireNonNull(other, "相减金额不能为空");
        return new Money(amount.subtract(other.amount));
    }

    public Money multiply(int times) {
        return new Money(amount.multiply(BigDecimal.valueOf(times)));
    }

    public BigDecimal getAmount() {
        return amount;
    }

    /** 数值是否相等（忽略 scale），与 {@link #equals} 语义一致，供显式比对的场景使用。 */
    public boolean sameAmount(Money other) {
        return other != null && amount.compareTo(other.amount) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
