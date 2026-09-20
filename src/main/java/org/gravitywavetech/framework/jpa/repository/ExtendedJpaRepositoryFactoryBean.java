package org.gravitywavetech.framework.jpa.repository;

import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import jakarta.persistence.EntityManager;
import java.io.Serializable;

/**
 * 自定义 {@link JpaRepositoryFactoryBean}
 *
 * <p>用于装配 {@link ExtendedJpaRepositoryFactory}，使所有仓储代理在创建时
 * 自动获得三个查询片段（Native / HQL / Criteria）的注入能力。</p>
 */
public class ExtendedJpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
        extends JpaRepositoryFactoryBean<T, S, ID> {

    public ExtendedJpaRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new ExtendedJpaRepositoryFactory(entityManager);
    }
}
