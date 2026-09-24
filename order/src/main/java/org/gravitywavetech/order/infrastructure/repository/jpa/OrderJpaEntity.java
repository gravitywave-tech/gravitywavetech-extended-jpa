package org.gravitywavetech.order.infrastructure.repository.jpa;

import jakarta.persistence.*;
import lombok.Data;
import org.gravitywavetech.order.domain.model.OrderStatus;

import java.math.BigDecimal;

/**
 * OrderJpaEntity
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@Entity
@Table(name="t_order")
@Data
public class OrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long buyerId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private BigDecimal totalAmount;

    private String province;
    private String city;
    private String detailAddress;
}
