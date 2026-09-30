package org.gravitywavetech.order.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.application.command.PayCommand;
import org.gravitywavetech.order.application.port.PaymentCreatePort;
import org.gravitywavetech.order.domain.model.Order;
import org.gravitywavetech.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;

/**
 * 订单支付应用服务。
 *
 * <p>微服务化改造后，本服务不再直接变更订单支付状态，而是向 payment 服务
 * 发送「创建支付单」消息；订单支付状态由 payment 服务完成后再通过
 * {@code PaymentSuccessConsumer} 回调驱动。</p>
 *
 * <p>依赖 {@link PaymentCreatePort} 出站端口而非具体 MQ 客户端，
 * 消息契约的组装是 infrastructure 层的实现细节。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private static final String DEFAULT_PAYMENT_METHOD = "ALIPAY";

    private final OrderRepository orderRepository;
    private final PaymentCreatePort paymentCreatePort;

    public void handlePayCommand(PayCommand command) {
        Order order = orderRepository.findById(command.orderId());
        paymentCreatePort.requestCreatePayment(
                order.getId(),
                order.getTotalAmount(),
                DEFAULT_PAYMENT_METHOD
        );
    }
}
