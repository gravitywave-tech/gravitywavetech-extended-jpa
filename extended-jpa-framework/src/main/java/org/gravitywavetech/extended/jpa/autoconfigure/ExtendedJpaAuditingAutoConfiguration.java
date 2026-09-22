package org.gravitywavetech.extended.jpa.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Spring Data JPA 审计自动配置。
 *
 * <p>{@link EnableJpaAuditing} 会激活 {@code AuditingEntityListener}，
 * 使继承 {@link org.gravitywavetech.extended.jpa.entity.BaseEntity} 的实体
 * 在 {@code persist / merge} 时自动填充 {@code @CreatedDate / @LastModifiedDate /
 * @CreatedBy / @LastModifiedBy} 四个字段。</p>
 *
 * <p>审计人来源由 {@link AuditorAware} 决定。当前默认实现返回
 * {@link Optional#empty()}，即 {@code creatorId / updatorId} 保持为 {@code null}
 * （时间字段仍会被填充）。业务方如需接入真实身份（Spring Security / 请求头 /
 * ThreadLocal 等），只需在自己的配置类里定义 {@code AuditorAware} Bean，
 * 本类因 {@link ConditionalOnMissingBean} 会自动让路。</p>
 *
 * <p><b>禁用开关</b>：设置 {@code extended.jpa.auditing.enabled=false} 可关闭审计接线。</p>
 *
 * @author gravitywavetech
 */
@AutoConfiguration
@ConditionalOnClass({ EnableJpaAuditing.class, AuditorAware.class })
@ConditionalOnProperty(prefix = "extended.jpa.auditing", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableJpaAuditing
public class ExtendedJpaAuditingAutoConfiguration {

    /**
     * 默认审计人来源：空。
     *
     * <p>若使用方已自定义 {@code AuditorAware} Bean，本方法因
     * {@link ConditionalOnMissingBean} 不会注册，避免覆盖。</p>
     */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> auditorAware() {
        return () -> Optional.empty();
    }
}