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

import java.util.function.Consumer;

/**
 * 消费 order 服务发出的创建支付单消息。
 *
 * <p>Spring Cloud Function 5.0.0 要求函数 Bean 实现 {@link Consumer} 等函数式接口。
 * Bean 名称必须等于 binding 前缀（{@code paymentCreate-in-0} → Bean 名 {@code paymentCreate}），
 * 通过 {@code spring.cloud.function.definition=paymentCreate} 声明函数名。</p>
 */
@Slf4j
@Component("paymentCreate")
@RequiredArgsConstructor
public class CreatePaymentConsumer implements Consumer<Message<PaymentCreateMessage>> {

    private final PaymentApplicationService paymentApplicationService;

    @Override
    public void accept(Message<PaymentCreateMessage> message) {
        PaymentCreateMessage payload = message.getPayload();
        log.info("消费创建支付单消息，orderId={}, amount={}, method={}",
                payload.orderId(), payload.amount(), payload.paymentMethod());

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
