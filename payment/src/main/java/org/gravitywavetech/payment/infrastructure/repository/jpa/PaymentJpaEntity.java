package org.gravitywavetech.payment.infrastructure.repository.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.gravitywavetech.payment.domain.model.PaymentMethod;
import org.gravitywavetech.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 支付单实体。
 */
@Entity
@Table(name = "t_payment")
@Data
public class PaymentJpaEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "order_ref_id", nullable = false)
    private Long orderRefId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 32)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentStatus status;

    @Column(name = "third_party_trade_no", length = 128)
    private String thirdPartyTradeNo;

    @Column(name = "paid_at")
    private Instant paidAt;
}
