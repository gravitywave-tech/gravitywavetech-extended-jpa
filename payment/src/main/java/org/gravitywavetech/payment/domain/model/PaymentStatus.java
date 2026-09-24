package org.gravitywavetech.payment.domain.model;

/**
 * PaymentStatus
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public enum PaymentStatus {
    PENDING,    // 待支付，已创建支付单，等待用户付款
    SUCCESS,    // 支付成功
    FAILED,     // 支付失败
    REFUNDED    // 已全额退款
}

