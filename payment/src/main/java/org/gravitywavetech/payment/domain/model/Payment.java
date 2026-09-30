package org.gravitywavetech.payment.domain.model;

/**
 * Payment
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
import org.gravitywavetech.payment.domain.event.PaymentCreatedEvent;
import org.gravitywavetech.payment.domain.event.PaymentFailedEvent;
import org.gravitywavetech.payment.domain.event.PaymentSuccessEvent;
import org.gravitywavetech.payment.domain.exception.PaymentStatusInvalidException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Payment {
    private PaymentId id;
    private OrderRef orderRef;
    private Money amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String thirdPartyTradeNo; // 第三方网关流水号
    private Instant paidAt;           // 支付成功时间

    // 待发布领域事件，聚合内暂存
    private final List<Object> domainEvents = new ArrayList<>();

    // 私有构造，只能通过工厂方法创建
    private Payment() {}

    /**
     * 工厂方法：创建支付单（PENDING待支付）
     */
    public static Payment create(PaymentId paymentId,
                                 OrderRef orderRef,
                                 Money amount,
                                 PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.id = paymentId;
        payment.orderRef = orderRef;
        payment.amount = amount;
        payment.paymentMethod = paymentMethod;
        payment.status = PaymentStatus.PENDING;
        payment.domainEvents.add(new PaymentCreatedEvent(paymentId, orderRef, amount, Instant.now()));
        return payment;
    }

    /**
     * 从持久化状态重建聚合（不触发 PaymentCreatedEvent，不重置状态）。
     *
     * <p>用于 Repository 加载已存在的支付单：数据库中的状态是合法的，
     * 不应重新走 create 工厂的初始化逻辑 —— 那会把 status 重置为 PENDING、
     * 丢失 thirdPartyTradeNo / paidAt，还会凭空产生一条 PaymentCreatedEvent，
     * 导致 markSuccess 的幂等守卫失效（同一笔支付可被重复标记成功）。</p>
     */
    public static Payment rehydrate(PaymentId paymentId,
                                    OrderRef orderRef,
                                    Money amount,
                                    PaymentMethod paymentMethod,
                                    PaymentStatus status,
                                    String thirdPartyTradeNo,
                                    Instant paidAt) {
        Payment payment = new Payment();
        payment.id = paymentId;
        payment.orderRef = orderRef;
        payment.amount = amount;
        payment.paymentMethod = paymentMethod;
        payment.status = status;
        payment.thirdPartyTradeNo = thirdPartyTradeNo;
        payment.paidAt = paidAt;
        return payment;
    }

    /**
     * 领域行为：网关回调 - 支付成功
     */
    public void markSuccess(String thirdPartyTradeNo) {
        // 不变量校验：只有待支付的才可以标记成功
        if(this.status != PaymentStatus.PENDING){
            throw new PaymentStatusInvalidException("支付单状态不是待支付，不能标记为成功");
        }
        this.status = PaymentStatus.SUCCESS;
        this.thirdPartyTradeNo = thirdPartyTradeNo;
        this.paidAt = Instant.now();
        domainEvents.add(new PaymentSuccessEvent(
                this.id,
                this.orderRef,
                this.amount,
                thirdPartyTradeNo,
                this.paidAt
        ));
    }

    /**
     * 领域行为：网关回调 - 支付失败
     */
    public void markFailed(String failReason) {
        if(this.status != PaymentStatus.PENDING){
            throw new PaymentStatusInvalidException("支付单状态不是待支付，不能标记为失败");
        }
        this.status = PaymentStatus.FAILED;
        domainEvents.add(new PaymentFailedEvent(this.id, this.orderRef, failReason, Instant.now()));
    }

    // 获取事件（只读集合）
    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    // 发布完成之后清空事件
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // ========= getter，只读，没有setter =========
    public PaymentId getId() { return id; }
    public OrderRef getOrderRef() { return orderRef; }
    public Money getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getThirdPartyTradeNo() { return thirdPartyTradeNo; }
    public Instant getPaidAt() { return paidAt; }
}
