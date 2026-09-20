package org.gravitywavetech.framework.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * HQL 查询片段实现。
 *
 * <p>构造参数携带所属仓储的实体类型，因此同一实现类可被所有仓储复用。</p>
 *
 * @param <T> 实体类型
 * @author Administrator
 * @version 1.0
 */
public class HqlQueryFragmentImpl<T> implements HqlQueryFragment<T> {

    private final EntityManager entityManager;
    private final Class<T> entityClass;

    public HqlQueryFragmentImpl(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    @Override
    public HqlQueryBuilder<T> hqlQuery() {
        return new HqlQueryBuilder<>(entityManager, entityClass);
    }

    @Override
    public List<T> findByHql(String hql, Object... params) {
        TypedQuery<T> query = entityManager.createQuery(hql, entityClass);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return query.getResultList();
    }

    @Override
    public Page<T> findByHql(String hql, Pageable pageable, Object... params) {
        // 统一走 SqlCountSupport，与 HqlQueryBuilder#count() 使用同一份 COUNT 生成规则
        TypedQuery<Long> countQuery = entityManager.createQuery(SqlCountSupport.buildCountHql(hql), Long.class);
        for (int i = 0; i < params.length; i++) {
            countQuery.setParameter(i + 1, params[i]);
        }
        long total = countQuery.getSingleResult();

        TypedQuery<T> query = entityManager.createQuery(hql, entityClass);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(query.getResultList(), pageable, total);
    }
}
