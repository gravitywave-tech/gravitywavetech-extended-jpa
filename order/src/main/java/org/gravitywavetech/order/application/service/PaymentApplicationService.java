package org.gravitywavetech.order.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.application.command.PayCommand;
import org.gravitywavetech.order.domain.model.Order;
import org.gravitywavetech.order.domain.repository.OrderRepository;
import org.gravitywavetech.order.infrastructure.client.PaymentCreateMessage;
import org.gravitywavetech.order.infrastructure.client.PaymentCreateProducer;
import org.springframework.stereotype.Service;

/**
 * 订单支付应用服务。
 *
 * <p>微服务化改造后，本服务不再直接变更订单支付状态，而是向 payment 服务
 * 发送「创建支付单」消息；订单支付状态由 payment 服务完成后再通过
 * {@code PaymentSuccessConsumer} 回调驱动。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private static final String DEFAULT_PAYMENT_METHOD = "ALIPAY";

    private final OrderRepository orderRepository;
    private final PaymentCreateProducer paymentCreateProducer;

    public void handlePayCommand(PayCommand command) {
        Order order = orderRepository.findById(command.orderId());
        PaymentCreateMessage message = PaymentCreateProducer.of(
                order.getId().getId(),
                command.amount().getAmount(),
                DEFAULT_PAYMENT_METHOD
        );
        paymentCreateProducer.sendCreatePayment(message);
    }
}
