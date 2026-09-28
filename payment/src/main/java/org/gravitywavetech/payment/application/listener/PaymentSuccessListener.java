package org.gravitywavetech.payment.application.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.payment.domain.event.PaymentSuccessEvent;
import org.gravitywavetech.payment.infrastructure.client.PaymentSuccessMessage;
import org.gravitywavetech.payment.infrastructure.client.PaymentSuccessProducer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 支付成功事件监听器。
 *
 * <p>微服务化改造后，本监听器不再跨服务访问 OrderRepository，
 * 而是向 order 服务发送 {@code PaymentSuccessMessage}，由 order 服务消费后驱动
 * Order 聚合的 pay() 领域行为，实现服务间最终一致性。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessListener {

    private final PaymentSuccessProducer paymentSuccessProducer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        PaymentSuccessMessage message = new PaymentSuccessMessage(
                String.valueOf(event.getPaymentId().getId()),
                event.getOrderRef().getOrderId(),
                event.getPaidAmount().getAmount(),
                event.getThirdPartyTradeNo(),
                event.getPaidAt()
        );
        paymentSuccessProducer.sendPaymentSuccess(message);
    }
}
