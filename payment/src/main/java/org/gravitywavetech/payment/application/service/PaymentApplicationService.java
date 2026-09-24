package org.gravitywavetech.payment.application.service;

import org.gravitywavetech.payment.application.command.CreatePaymentCommand;
import org.gravitywavetech.payment.domain.model.Payment;
import org.gravitywavetech.payment.domain.model.PaymentId;
import org.gravitywavetech.payment.domain.repository.PaymentRepository;
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
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentApplicationService(PaymentRepository paymentRepository, ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建支付单
     */
    @Transactional
    public Payment createPayment(CreatePaymentCommand command) {
        // 调用聚合工厂方法创建Payment聚合
        Payment payment = Payment.create(
                command.paymentId(),
                command.orderRef(),
                command.amount(),
                command.paymentMethod()
        );
        paymentRepository.save(payment);
        // 发布领域事件
        payment.getDomainEvents().forEach(eventPublisher::publishEvent);
        payment.clearDomainEvents();
        return payment;
    }

    /**
     * 支付网关回调入口
     */
    @Transactional
    public void handlePaymentCallback(PaymentId paymentId, String tradeNo, boolean paySuccess, String failMsg) {
        Payment payment = paymentRepository.findById(paymentId);
        if(paySuccess){
            payment.markSuccess(tradeNo);
        }else{
            payment.markFailed(failMsg);
        }
        paymentRepository.save(payment);
        payment.getDomainEvents().forEach(eventPublisher::publishEvent);
        payment.clearDomainEvents();
    }
}

