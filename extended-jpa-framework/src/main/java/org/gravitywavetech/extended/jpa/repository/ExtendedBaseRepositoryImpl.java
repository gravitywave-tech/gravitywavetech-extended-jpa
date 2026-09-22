package org.gravitywavetech.extended.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.repository.NoRepositoryBean;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 扩展通用仓储实现类 —— `repositoryBaseClass`。
 *
 * <p>本类非常薄，只做一件事：<b>把三类查询能力委托给对应的查询片段实现</b>。
 * CRUD 与审计逻辑在 {@link BaseRepositoryImpl}，查询逻辑在各 Fragment 实现类，
 * 因此这里不再承载任何具体实现细节，也不再需要 {@code buildCountSql()} 之类的私有方法。</p>
 *
 * <p>为什么保留基类（而不是纯 Fragment 装配）：</p>
 * <ul>
 *     <li>基类天然携带 {@code T} / {@code ID}，因此 {@link NativeQueryFragmentImpl} 等片段实现
 *         可以用 {@code Class<T> domainClass} 精确实例化，返回值保持
 *         {@code NativeQueryBuilder<T>} / {@code List<T>} / {@code Page<T>}，无任何强转。</li>
 *     <li>只有基类可以重写 {@code save()} / {@code findAll()} 等既有方法，Fragment 无法覆盖。</li>
 *     <li>片段实现类与基类解耦：同一实现可被 {@link ExtendedJpaRepositoryFactory}
 *         注入给不继承 {@link ExtendedBaseRepository} 的普通 JpaRepository，实现按需装配。</li>
 * </ul>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author Administrator
 * @version 1.0
 * @since 2026/8/20 16:09
 */
@NoRepositoryBean
public class ExtendedBaseRepositoryImpl<T, ID extends Serializable> extends BaseRepositoryImpl<T, ID>
        implements ExtendedBaseRepository<T, ID> {

    private final NativeQueryFragment<T> nativeQueryFragment;
    private final HqlQueryFragment<T> hqlQueryFragment;
    private final CriteriaQueryFragment<T> criteriaQueryFragment;

    /**
     * 构造函数 - 匹配父类的签名，供 Spring Data 反射调用
     */
    public ExtendedBaseRepositoryImpl(JpaEntityInformation<T, Serializable> entityInformation,
                                      EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.nativeQueryFragment = new NativeQueryFragmentImpl<>(entityManager, domainClass);
        this.hqlQueryFragment = new HqlQueryFragmentImpl<>(entityManager, domainClass);
        this.criteriaQueryFragment = new CriteriaQueryFragmentImpl<>(entityManager, domainClass);
    }

    /**
     * 构造函数 - 使用实体类
     */
    @SuppressWarnings("unchecked")
    public ExtendedBaseRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        this((JpaEntityInformation<T, Serializable>) JpaEntityInformationSupport
                .getEntityInformation(domainClass, entityManager), entityManager);
    }

    // ==================== Native SQL 查询 ====================

    @Override
    public NativeQueryBuilder<T> nativeQuery() {
        return nativeQueryFragment.nativeQuery();
    }

    @Override
    public <R> List<R> findByNativeSql(String sql, Class<R> resultClass, Object... params) {
        return nativeQueryFragment.findByNativeSql(sql, resultClass, params);
    }

    @Override
    public <R> Page<R> findByNativeSql(String sql, Class<R> resultClass, Pageable pageable,
                                       Object... params) {
        return nativeQueryFragment.findByNativeSql(sql, resultClass, pageable, params);
    }

    @Override
    public List<Map<String, Object>> findByNativeSqlForMap(String sql, Object... params) {
        return nativeQueryFragment.findByNativeSqlForMap(sql, params);
    }

    @Override
    public Page<Map<String, Object>> findByNativeSqlForMap(String sql, Pageable pageable,
                                                           Object... params) {
        return nativeQueryFragment.findByNativeSqlForMap(sql, pageable, params);
    }

    // ==================== HQL 查询 ====================

    @Override
    public HqlQueryBuilder<T> hqlQuery() {
        return hqlQueryFragment.hqlQuery();
    }

    @Override
    public List<T> findByHql(String hql, Object... params) {
        return hqlQueryFragment.findByHql(hql, params);
    }

    @Override
    public Page<T> findByHql(String hql, Pageable pageable, Object... params) {
        return hqlQueryFragment.findByHql(hql, pageable, params);
    }

    // ==================== Criteria API 查询 ====================

    @Override
    public CriteriaQueryBuilder<T> criteriaQuery() {
        return criteriaQueryFragment.criteriaQuery();
    }

    @Override
    public Page<T> findByCriteria(CriteriaQueryBuilder<T> builder, Pageable pageable) {
        return criteriaQueryFragment.findByCriteria(builder, pageable);
    }
}
