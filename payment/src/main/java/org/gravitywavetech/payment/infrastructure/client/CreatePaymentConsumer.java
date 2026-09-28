package org.gravitywavetech.payment.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.payment.application.command.CreatePaymentCommand;
import org.gravitywavetech.payment.application.service.PaymentApplicationService;
import org.gravitywavetech.payment.domain.model.Money;
import org.gravitywavetech.payment.domain.model.OrderRef;
import org.gravitywavetech.payment.domain.model.Payment;
import org.gravitywavetech.payment.domain.model.PaymentId;
import org.gravitywavetech.payment.domain.model.PaymentMethod;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 消费 order 服务发出的创建支付单请求。
 *
 * <p>消息由 RabbitMQ 投递，本消费者在同一进程内驱动 Payment 聚合创建。
 * PaymentId 由雪花/UUID 生成，避免与 orderId 冲突。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreatePaymentConsumer {

    private final PaymentApplicationService paymentApplicationService;

    public void accept(Message<PaymentCreateMessage> message) {
        PaymentCreateMessage payload = message.getPayload();
        log.info("消费创建支付单消息，orderId={}, amount={}, method={}",
                payload.orderId(), payload.amount(), payload.paymentMethod());

        // 用时间戳 + 随机数生成唯一 PaymentId，避免消息重复投递导致主键冲突
        PaymentId paymentId = new PaymentId(System.currentTimeMillis());
        PaymentMethod method = PaymentMethod.valueOf(payload.paymentMethod());

        CreatePaymentCommand command = new CreatePaymentCommand(
                paymentId,
                new OrderRef(payload.orderId()),
                Money.of(payload.amount()),
                method
        );
        Payment payment = paymentApplicationService.createPayment(command);
        log.info("支付单创建完成，paymentId={}", payment.getId().getId());
    }
}
