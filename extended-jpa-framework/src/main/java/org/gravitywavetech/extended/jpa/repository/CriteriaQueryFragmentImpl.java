package org.gravitywavetech.extended.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;

/**
 * Criteria API 查询片段实现。
 *
 * <p>构造参数携带所属仓储的实体类型，因此同一实现类可被所有仓储复用。</p>
 *
 * @param <T> 实体类型
 * @author Administrator
 * @version 1.0
 */
public class CriteriaQueryFragmentImpl<T> implements CriteriaQueryFragment<T> {

    private final EntityManager entityManager;
    private final Class<T> entityClass;

    public CriteriaQueryFragmentImpl(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    @Override
    public CriteriaQueryBuilder<T> criteriaQuery() {
        return new CriteriaQueryBuilder<>(entityManager, entityClass);
    }

    @Override
    public Page<T> findByCriteria(CriteriaQueryBuilder<T> builder, Pageable pageable) {
        return builder.page(pageable);
    }
}
