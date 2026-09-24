package org.gravitywavetech.payment.infrastructure.repository.jpa;
import jakarta.persistence.*;
import lombok.Data;
import org.gravitywavetech.payment.domain.model.PaymentMethod;
import org.gravitywavetech.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
/**
 * PaymentJpaEntity
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@Entity
@Table(name="t_payment")
@Data
public class PaymentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderRefId;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    private String thirdPartyTradeNo;
}
