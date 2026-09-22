package org.gravitywavetech.extended.jpa.autoconfigure;

import org.gravitywavetech.extended.jpa.repository.ExtendedBaseRepositoryImpl;
import org.gravitywavetech.extended.jpa.repository.ExtendedJpaRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;

import jakarta.persistence.EntityManagerFactory;

/**
 * Extended JPA starter 自动配置。
 *
 * <p>注册两类扩展点：</p>
 * <ol>
 *     <li>{@code repositoryBaseClass = ExtendedBaseRepositoryImpl}：所有仓储以本类为基类，
 *         由此获得 {@link org.gravitywavetech.extended.jpa.repository.ExtendedBaseRepository}
 *         声明的 Native / HQL / Criteria 三类查询能力，并保留泛型与覆盖 CRUD 的能力。</li>
 *     <li>{@code repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean}：为
 *         <b>不</b>继承 {@code ExtendedBaseRepository} 的普通 {@code JpaRepository} 注入查询片段实现，
 *         实现按需装配（见 {@code ExtendedJpaRepositoryFactory#getRepositoryFragments}）。</li>
 * </ol>
 *
 * <p><b>basePackages</b> 留空，Spring Data 会从 {@code AutoConfigurationPackages} 取使用方
 * {@code @SpringBootApplication} 所在包扫描仓储，因此把本 starter 加进任意 Spring Boot 应用即可自动接线。</p>
 *
 * <p><b>让路机制</b>：Spring Boot 自带的 {@code JpaRepositoriesAutoConfiguration} 带有
 * {@code @ConditionalOnMissingBean(JpaRepositoryFactoryBean.class)}，本类注入的
 * {@link ExtendedJpaRepositoryFactoryBean} 是其子类，因此 Boot 自动配置会自动让路。
 * {@code @AutoConfigureBefore} 显式声明顺序，确保我们的 FactoryBean 先注册。</p>
 *
 * <p><b>禁用开关</b>：设置 {@code extended.jpa.enabled=false} 可整体关闭本 starter。</p>
 *
 * @author gravitywavetech
 */
@AutoConfiguration
@ConditionalOnClass({ JpaRepository.class, EntityManagerFactory.class })
@ConditionalOnProperty(prefix = "extended.jpa", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean({ JpaRepositoryConfigExtension.class })
@Import(ExtendedJpaRepositoriesRegistrar.class)
public class ExtendedJpaAutoConfiguration {
}