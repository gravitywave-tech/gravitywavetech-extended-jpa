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
     * 领域行为：网关回调 - 支付成功
     */
    public void markSuccess(String thirdPartyTradeNo) {
        // 不变量校验：只有待支付的才可以标记成功
        if(this.status != PaymentStatus.PENDING){
            throw new PaymentStatusInvalidException("支付单状态不是待支付，不能标记为成功");
        }
        this.status = PaymentStatus.SUCCESS;
        this.thirdPartyTradeNo = thirdPartyTradeNo;
        domainEvents.add(new PaymentSuccessEvent(
                this.id,
                this.orderRef,
                this.amount,
                thirdPartyTradeNo,
                Instant.now()
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
}
