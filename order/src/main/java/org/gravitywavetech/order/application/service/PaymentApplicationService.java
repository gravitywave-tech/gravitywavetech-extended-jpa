package org.gravitywavetech.application.service;

import org.gravitywavetech.application.command.PayCommand;
import org.gravitywavetech.domain.model.Order;
import org.gravitywavetech.domain.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PaymentApplicationService
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@Service
public class PaymentApplicationService {
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentApplicationService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void handlePayCommand(PayCommand command) {
        Order order = orderRepository.findById(command.orderId());
        // 调用聚合根领域行为，业务规则全部在Order内部
        order.pay(command.amount());
        // 保存聚合
        orderRepository.save(order);
        // 发布所有领域事件
        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        // 清空事件，防止重复发布
        order.clearDomainEvents();
    }
}
