package org.gravitywavetech.extended.jpa.autoconfigure;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.gravitywavetech.extended.jpa.repository.ExtendedBaseRepositoryImpl;
import org.gravitywavetech.extended.jpa.repository.ExtendedJpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * Custom {@code ImportBeanDefinitionRegistrar} that uses
 * {@link org.springframework.boot.autoconfigure.AutoConfigurationPackages} to resolve
 * repository base packages, following the same pattern as Spring Boot's own
 * {@code DataJpaRepositoriesAutoConfiguration}.
 *
 * <p>Without this registrar, {@code @EnableJpaRepositories} on an
 * {@code @AutoConfiguration} class would scan the auto-configuration class's own package
 * instead of the application's base package.</p>
 */
class ExtendedJpaRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableJpaRepositories.class;
    }

    @Override
    protected Class<?> getConfiguration() {
        return ExtendedJpaRepositoriesConfiguration.class;
    }

    @Override
    protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
        return new JpaRepositoryConfigExtension();
    }

    @EnableJpaRepositories(
            repositoryBaseClass = ExtendedBaseRepositoryImpl.class,
            repositoryFactoryBeanClass = ExtendedJpaRepositoryFactoryBean.class)
    private static final class ExtendedJpaRepositoriesConfiguration {
    }
}