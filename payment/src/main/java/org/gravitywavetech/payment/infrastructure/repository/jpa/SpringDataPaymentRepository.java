package org.gravitywavetech.payment.infrastructure.repository.jpa;

import org.gravitywavetech.payment.domain.exception.PaymentNotFoundException;
import org.gravitywavetech.payment.domain.model.Money;
import org.gravitywavetech.payment.domain.model.OrderRef;
import org.gravitywavetech.payment.domain.model.Payment;
import org.gravitywavetech.payment.domain.model.PaymentId;
import org.gravitywavetech.payment.domain.repository.PaymentRepository;
import org.springframework.stereotype.Repository;

/**
 * SpringDataPaymentRepository
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@Repository
public class SpringDataPaymentRepository implements PaymentRepository {
    private final PaymentJpaRepository jpaRepository;

    public SpringDataPaymentRepository(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment findById(PaymentId paymentId) {
        PaymentJpaEntity entity = jpaRepository.findById(paymentId.getId())
                .orElseThrow(() -> new PaymentNotFoundException(paymentId.getId()));
        // 用 rehydrate 而非 create：恢复 DB 中的真实状态（SUCCESS/FAILED），
        // 不重置为 PENDING，也不触发多余的 PaymentCreatedEvent
        return Payment.rehydrate(
                new PaymentId(entity.getId()),
                new OrderRef(entity.getOrderRefId()),
                Money.of(entity.getAmount()),
                entity.getPaymentMethod(),
                entity.getStatus(),
                entity.getThirdPartyTradeNo(),
                entity.getPaidAt()
        );
    }

    @Override
    public void save(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(payment.getId().getId());
        entity.setOrderRefId(payment.getOrderRef().getOrderId());
        entity.setAmount(payment.getAmount().getAmount());
        entity.setPaymentMethod(payment.getPaymentMethod());
        entity.setStatus(payment.getStatus());
        entity.setThirdPartyTradeNo(payment.getThirdPartyTradeNo());
        entity.setPaidAt(payment.getPaidAt());
        jpaRepository.save(entity);
    }
}
