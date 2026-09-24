package org.gravitywavetech.payment.application.listener;

import org.gravitywavetech.order.domain.model.OrderId;
import org.gravitywavetech.order.domain.repository.OrderRepository;
import org.gravitywavetech.payment.domain.event.PaymentSuccessEvent;
import org.gravitywavetech.payment.domain.model.Money;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * PaymentSuccessListener
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@Component
public class PaymentSuccessListener {
    private final OrderRepository orderRepository;
    // 注入事件发布器
    private final ApplicationEventPublisher eventPublisher;

    public PaymentSuccessListener(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 订单上下文监听【支付上下文】发布的PaymentSuccessEvent
     * 支付上下文事务提交后，触发；单独事务，最终一致性
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccess(PaymentSuccessEvent event){
        Long orderIdVal = event.getOrderRef().getOrderId();
        OrderId orderId = new OrderId(orderIdVal);
        Money paidMoney = event.getPaidAmount();

        // 加载订单聚合，调用订单领域行为pay()
        var order = orderRepository.findById(orderId);
        //order.pay(paidMoney);
        orderRepository.save(order);
        // 订单内部会发布OrderPaidEvent，再去扣库存、发短信
        order.getDomainEvents().forEach(e -> eventPublisher.publishEvent(e));
        order.clearDomainEvents();
    }
}

