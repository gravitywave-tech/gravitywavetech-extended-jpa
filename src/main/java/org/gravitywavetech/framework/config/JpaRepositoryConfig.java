package org.gravitywavetech.framework.config;

import org.gravitywavetech.framework.jpa.repository.ExtendedBaseRepositoryImpl;
import org.gravitywavetech.framework.jpa.repository.ExtendedJpaRepositoryFactoryBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 仓储接线配置。
 *
 * <p>本项目提供了两套扩展点，此前均未接线，本配置负责将它们真正启用：</p>
 * <ol>
 *     <li>{@code repositoryBaseClass = ExtendedBaseRepositoryImpl}：所有仓储以本类为基类，
 *         由此获得 {@link org.gravitywavetech.framework.jpa.repository.ExtendedBaseRepository}
 *         声明的 Native / HQL / Criteria 三类查询能力，并保留泛型与覆盖 CRUD 的能力。</li>
 *     <li>{@code repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean}：为
 *         <b>不</b>继承 {@code ExtendedBaseRepository} 的普通 {@code JpaRepository} 注入查询片段实现，
 *         实现按需装配（见 {@code ExtendedJpaRepositoryFactory#getRepositoryFragments}）。</li>
 * </ol>
 *
 * <p>关于 Spring Boot 自动配置：{@code JpaRepositoriesAutoConfiguration} 带有
 * {@code @ConditionalOnMissingBean(JpaRepositoryFactoryBean.class)}，本配置注册的
 * {@code ExtendedJpaRepositoryFactoryBean} 是其子类，因此自动配置会自动让路，不会重复注册仓储。</p>
 *
 * <p><b>注意</b>：{@code basePackages} 目前写死为框架根包。后续若本工程拆分为 spring-boot-starter，
 * 应改为在自动配置类中通过 {@code AutoConfigurationPackages.get(beanFactory)} 动态获取使用方包名。</p>
 *
 * @author gravitywavetech
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
        basePackages = "org.gravitywavetech.framework",
        repositoryBaseClass = ExtendedBaseRepositoryImpl.class,
        repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean.class)
public class JpaRepositoryConfig {
}
