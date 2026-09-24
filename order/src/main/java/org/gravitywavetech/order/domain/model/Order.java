package org.gravitywavetech.order.domain.model;

/**
 * Order
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */

import org.gravitywavetech.order.domain.event.OrderCancelledWithRefundEvent;
import org.gravitywavetech.order.domain.event.OrderCreatedEvent;
import org.gravitywavetech.order.domain.exception.InvalidOrderStateException;
import org.gravitywavetech.order.domain.exception.PaymentAmountMismatchException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private OrderId id;
    private Long buyerId;
    private List<OrderItem> items;
    private OrderStatus status;
    private Money totalAmount;
    private Address shippingAddress;

    // 聚合内部暂存待发布领域事件
    private final List<Object> domainEvents = new ArrayList<>();

    // 私有构造，只能用工厂方法创建
    private Order(){}

    // 工厂方法：新建订单
    public static Order create(OrderId id, Long buyerId, List<OrderItem> items, Address address){
        Order order = new Order();
        order.id = id;
        order.buyerId = buyerId;
        order.items = new ArrayList<>(items);
        order.shippingAddress = address;
        order.status = OrderStatus.WAITING_PAYMENT;
        // 汇总总金额
        Money total = Money.of(BigDecimal.ZERO);
        for(OrderItem item : items){
            total = Money.of(total.getAmount().add(item.getSubTotal().getAmount()));
        }
        order.totalAmount = total;
        order.domainEvents.add(new OrderCreatedEvent(id));
        return order;
    }

    /**
     * 领域行为：支付（原文示例）
     */
    public void pay(Money paidAmount) {
        ensureStatus(OrderStatus.WAITING_PAYMENT);
        if (!totalAmount.equals(paidAmount)) {
            throw new PaymentAmountMismatchException();
        }
        this.status = OrderStatus.PAID;
        domainEvents.add(new OrderPaidEvent(id, Instant.now()));
    }

    /**
     * 领域行为：取消订单（原文重点代码分支）
     */
    public void cancel() {
        if (status == OrderStatus.PAID) {
            // 已支付订单取消，触发退款事件
            domainEvents.add(new OrderCancelledWithRefundEvent(id));
            this.status = OrderStatus.CANCELLED;
        } else if (status == OrderStatus.WAITING_PAYMENT) {
            this.status = OrderStatus.CANCELLED;
        } else {
            throw new InvalidOrderStateException("当前状态不可取消");
        }
    }

    public void ship(){
        ensureStatus(OrderStatus.PAID);
        this.status = OrderStatus.SHIPPED;
    }

    public void complete(){
        ensureStatus(OrderStatus.SHIPPED);
        this.status = OrderStatus.COMPLETED;
    }

    private void ensureStatus(OrderStatus expected) {
        if (status != expected) {
            throw new InvalidOrderStateException("当前状态不是 " + expected);
        }
    }

    // 对外返回不可修改的事件列表
    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    // 事件发布完成后清空
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // getter，只暴露只读信息，没有setStatus
    public OrderId getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public Money getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public Address getShippingAddress() { return shippingAddress; }
    public Long getBuyerId() { return buyerId; }
}
