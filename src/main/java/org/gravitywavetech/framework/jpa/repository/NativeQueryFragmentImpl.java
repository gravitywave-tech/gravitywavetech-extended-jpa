package org.gravitywavetech.framework.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 原生 SQL 查询片段实现。
 *
 * <p>构造参数携带所属仓储的实体类型，因此同一实现类可被所有仓储复用，
 * 既能被 {@link ExtendedJpaRepositoryFactory} 注入给普通 JpaRepository，
 * 也能被 {@link ExtendedBaseRepositoryImpl} 组合委托。</p>
 *
 * @param <T> 实体类型
 * @author Administrator
 * @version 1.0
 */
public class NativeQueryFragmentImpl<T> implements NativeQueryFragment<T> {

    private final EntityManager entityManager;
    private final Class<T> entityClass;

    public NativeQueryFragmentImpl(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    @Override
    public NativeQueryBuilder<T> nativeQuery() {
        return new NativeQueryBuilder<>(entityManager, entityClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> List<R> findByNativeSql(String sql, Class<R> resultClass, Object... params) {
        Query query = entityManager.createNativeQuery(sql, resultClass);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Page<R> findByNativeSql(String sql, Class<R> resultClass,
                                       Pageable pageable, Object... params) {
        long total = count(sql, params);

        Query query = entityManager.createNativeQuery(sql, resultClass);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<R> content = query.getResultList();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Map<String, Object>> findByNativeSqlForMap(String sql, Object... params) {
        Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return toMapList(query.getResultList());
    }

    @Override
    public Page<Map<String, Object>> findByNativeSqlForMap(String sql, Pageable pageable,
                                                           Object... params) {
        long total = count(sql, params);

        Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(toMapList(query.getResultList()), pageable, total);
    }

    /**
     * 统一走 {@link SqlCountSupport}，与 {@link NativeQueryBuilder} 使用同一份 COUNT 生成规则
     */
    private long count(String sql, Object... params) {
        Query countQuery = entityManager.createNativeQuery(SqlCountSupport.buildCountSql(sql));
        for (int i = 0; i < params.length; i++) {
            countQuery.setParameter(i + 1, params[i]);
        }
        return ((Number) countQuery.getSingleResult()).longValue();
    }

    /**
     * 兼容单列返回（非 Object[]）的情况
     */
    private List<Map<String, Object>> toMapList(List<?> rawResults) {
        return rawResults.stream()
                .map(row -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    if (row instanceof Object[]) {
                        Object[] arr = (Object[]) row;
                        for (int i = 0; i < arr.length; i++) {
                            map.put("col_" + i, arr[i]);
                        }
                    } else {
                        map.put("col_0", row);
                    }
                    return map;
                })
                .collect(Collectors.toList());
    }
}
