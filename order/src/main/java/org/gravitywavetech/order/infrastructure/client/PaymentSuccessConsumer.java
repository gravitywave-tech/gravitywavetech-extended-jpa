package org.gravitywavetech.order.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.domain.model.Money;
import org.gravitywavetech.order.domain.model.Order;
import org.gravitywavetech.order.domain.model.OrderId;
import org.gravitywavetech.order.domain.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

/**
 * 消费 payment 服务发出的支付成功消息，驱动 Order 聚合的 pay() 领域行为。
 *
 * <p>Bean 名称必须等于 binding 前缀（{@code paymentSuccess-in-0} → Bean 名 {@code paymentSuccess}），
 * 通过 {@code spring.cloud.function.definition=paymentSuccess} 声明函数名。</p>
 */
@Slf4j
@Component("paymentSuccess")
@RequiredArgsConstructor
public class PaymentSuccessConsumer implements Consumer<Message<PaymentSuccessMessage>> {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public void accept(Message<PaymentSuccessMessage> message) {
        PaymentSuccessMessage payload = message.getPayload();
        log.info("消费支付成功消息，paymentId={}, orderId={}, tradeNo={}",
                payload.paymentId(), payload.orderId(), payload.thirdPartyTradeNo());

        OrderId orderId = new OrderId(payload.orderId());
        Money paidMoney = Money.of(payload.paidAmount());

        Order order = orderRepository.findById(orderId);
        order.pay(paidMoney);
        orderRepository.save(order);

        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        order.clearDomainEvents();
    }
}
