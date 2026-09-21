package org.gravitywavetech.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Spring Data JPA 审计接线配置。
 *
 * <p>{@link EnableJpaAuditing} 会激活 {@code AuditingEntityListener}，
 * 使继承 {@link org.gravitywavetech.framework.jpa.entity.BaseEntity} 的实体
 * 在 {@code persist / merge} 时自动填充 {@code @CreatedDate / @LastModifiedDate /
 * @CreatedBy / @LastModifiedBy} 四个字段。</p>
 *
 * <p>审计人来源由 {@link AuditorAware} 决定。当前默认实现返回 {@link Optional#empty()}，
 * 即 {@code creatorId / updatorId} 保持为 {@code null}（时间字段仍会被填充）。
 * 后续接入真实身份认证（Spring Security / 请求头 / ThreadLocal 等）时，只需替换此 Bean，
 * 业务代码无需改动。</p>
 *
 * @author gravitywavetech
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.empty();
    }
}