package org.gravitywavetech.payment.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.payment.application.command.CreatePaymentCommand;
import org.gravitywavetech.payment.domain.model.Payment;
import org.gravitywavetech.payment.domain.model.PaymentId;
import org.gravitywavetech.payment.domain.repository.PaymentRepository;
import org.gravitywavetech.payment.infrastructure.client.PaymentSuccessMessage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付应用服务。
 *
 * <p>负责编排「创建支付单 / 处理网关回调」两个用例。
 *
 * <p>微服务化 + Outbox 模式后，支付成功回调不再通过 {@code @TransactionalEventListener}
 * 直接向 RabbitMQ 发送消息（该方式在 broker 故障时会丢消息），
 * 而是把消息体与业务状态一起写入本地消息表 {@code t_outbox_message}，
 * 由 {@code OutboxDispatcher} 定时可靠投递。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    /** Outbox 写入目标：Spring Cloud Stream output binding → order.payment-callback queue */
    public static final String OUTBOX_DESTINATION_PAYMENT_SUCCESS = "paymentSuccess-out-0";

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxService outboxService;

    /**
     * 创建支付单。
     */
    @Transactional
    public Payment createPayment(CreatePaymentCommand command) {
        Payment payment = Payment.create(
                command.paymentId(),
                command.orderRef(),
                command.amount(),
                command.paymentMethod()
        );
        paymentRepository.save(payment);
        payment.getDomainEvents().forEach(eventPublisher::publishEvent);
        payment.clearDomainEvents();
        return payment;
    }

    /**
     * 支付网关回调入口。
     *
     * <p>在同一事务内完成：
     * <ol>
     *   <li>更新 Payment 聚合状态（成功/失败）</li>
     *   <li>发布领域事件（进程内通知）</li>
     *   <li>支付成功时写入 Outbox（跨服务消息，异步可靠投递到 order 服务）</li>
     * </ol>
     * 三者要么一起成功，要么一起回滚。
     */
    @Transactional
    public void handlePaymentCallback(PaymentId paymentId, String tradeNo, boolean paySuccess, String failMsg) {
        Payment payment = paymentRepository.findById(paymentId);
        if (paySuccess) {
            payment.markSuccess(tradeNo);
        } else {
            payment.markFailed(failMsg);
        }
        paymentRepository.save(payment);

        // 发布进程内领域事件
        payment.getDomainEvents().forEach(eventPublisher::publishEvent);
        payment.clearDomainEvents();

        // 跨服务消息：仅支付成功需要通知 order 服务
        if (paySuccess) {
            writePaymentSuccessOutbox(payment);
        }

        log.info("支付回调处理完成，paymentId={}, status={}", paymentId.getId(), payment.getStatus());
    }

    private void writePaymentSuccessOutbox(Payment payment) {
        PaymentSuccessMessage message = new PaymentSuccessMessage(
                String.valueOf(payment.getId().getId()),
                payment.getOrderRef().getOrderId(),
                payment.getAmount().getAmount(),
                payment.getThirdPartyTradeNo(),
                payment.getPaidAt()
        );
        String messageKey = "payment.success." + payment.getId().getId();
        outboxService.publish(messageKey, OUTBOX_DESTINATION_PAYMENT_SUCCESS, message);
        log.info("支付成功消息已写入 Outbox，paymentId={}, messageKey={}",
                payment.getId().getId(), messageKey);
    }
}
