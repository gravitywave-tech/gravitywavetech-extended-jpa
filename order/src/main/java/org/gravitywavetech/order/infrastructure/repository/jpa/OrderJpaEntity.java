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
    // 不使用 @GeneratedValue：主键由 OrderApplicationService 通过 SnowflakeUtil 生成
    // 否则 BaseRepositoryImpl 检测到 hasGeneratedValue=true 时不会自动填充，但 Hibernate
    // 在 IDENTITY 策略下会忽略传入 id 而使用数据库自增，导致 Order 域模型 id 与 DB 不一致。
    @Id
    private Long id;

    private Long buyerId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private BigDecimal totalAmount;

    private String province;
    private String city;
    private String detailAddress;
}
