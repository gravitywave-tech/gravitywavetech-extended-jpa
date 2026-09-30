package org.gravitywavetech.extended.jpa.repository;

import org.gravitywavetech.order.infrastructure.repository.jpa.OrderJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ExtendedBaseRepositoryImpl} 单元测试 —— 验证「方案一：基类骨架 + 片段拆分」的装配正确性。
 *
 * <p>本测试不重复覆盖查询构建逻辑（那是各 Fragment 实现的测试职责），只验证：</p>
 * <ol>
 *   <li><b>委托正确</b>：三个查询入口确实转发给了对应的 Fragment 实现；</li>
 *   <li><b>泛型保留</b>：返回值是带实体类型的 {@code NativeQueryBuilder<T> / HqlQueryBuilder<T> /
 *       Page<T>}，无需强转；</li>
 *   <li><b>COUNT 统一</b>：分页 COUNT 与构建器共用 {@link SqlCountSupport}；</li>
 *   <li><b>CRUD 分离</b>：{@code save()} 只负责 ID 生成与 persist/merge 分发；
 *       审计字段由 Spring Data Auditing 在 {@code persist / merge} 前通过
 *       {@code AuditingEntityListener} 自动填充，本单元测试无 Spring 上下文故不覆盖该部分。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
public class ExtendedBaseRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    /** SimpleJpaRepository 构造函数会通过 EMF 探测 PersistenceProvider，需提供一个非空实例 */
    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private JpaEntityInformation<OrderJpaEntity, Serializable> orderJpaEntityInformation;


    /** findByNativeSql 的数据查询 */
    @Mock
    private Query dataQuery;

    /** 原生 COUNT 查询 */
    @Mock
    private Query countQuery;

    @Mock
    private TypedQuery<OrderJpaEntity> typedQuery;

    @Mock
    private TypedQuery<Long> typedCountQuery;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<OrderJpaEntity> criteriaQuery;

    @Mock
    private Root<OrderJpaEntity> root;

    /** 被测对象：以 df_api_dict_item 为业务场景 */
    private ExtendedBaseRepositoryImpl<OrderJpaEntity, Long> orderJpaEntityRepository;



    @BeforeEach
    public void setUp() {
        when(entityManager.getEntityManagerFactory()).thenReturn(entityManagerFactory);

        // 基类通过 JpaEntityInformation 拿到实体类型，这是泛型得以保留的关键
        when(orderJpaEntityInformation.getJavaType()).thenReturn(OrderJpaEntity.class);

        orderJpaEntityRepository = new ExtendedBaseRepositoryImpl<>(orderJpaEntityInformation, entityManager);
    }

    // ==================== 三个查询入口的委托与泛型 ====================

    /** nativeQuery() 应返回已注入实体类型的构建器（委托到 NativeQueryFragmentImpl） */
    @Test
    public void nativeQuery_delegatesAndKeepsEntityType() {
        NativeQueryBuilder<OrderJpaEntity> builder = orderJpaEntityRepository.nativeQuery()
                .select("*")
                .from("t_order")
                .eq("delete_status", "N");

        assertEquals("SELECT * FROM t_order WHERE delete_status = ?1 ", builder.getSql());
        assertEquals(Collections.singletonList("N"), builder.getParams());
    }

    /** hqlQuery() 应返回已注入实体类型的构建器（委托到 HqlQueryFragmentImpl） */
    @Test
    public void hqlQuery_delegatesAndKeepsEntityType() {
        HqlQueryBuilder<OrderJpaEntity> builder = orderJpaEntityRepository.hqlQuery()
                .from()
                .eq("province", "fxtw_fl");

        assertEquals("FROM t_order t WHERE t.province = ?1 ", builder.getHql());
    }

    /** criteriaQuery() 应返回已注入实体类型的构建器（委托到 CriteriaQueryFragmentImpl） */
    @Test
    public void criteriaQuery_delegatesAndKeepsEntityType() {
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(OrderJpaEntity.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(OrderJpaEntity.class)).thenReturn(root);

        CriteriaQueryBuilder<OrderJpaEntity> builder = orderJpaEntityRepository.criteriaQuery();

        assertNotNull(builder);
        verify(entityManager).getCriteriaBuilder();
    }

    // ==================== 执行入口的委托 ====================

    /** findByNativeSql 直接委托给 Native 片段，返回 List<DfApiDictItem> */
    @Test
    public void findByNativeSql_delegatesToNativeFragment() {
        String sql = "SELECT * FROM t_order WHERE province = ?1";
        OrderJpaEntity expected = new OrderJpaEntity();
        expected.setId(1L);

        when(entityManager.createNativeQuery(sql, OrderJpaEntity.class)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(Collections.singletonList(expected));

        List<OrderJpaEntity> result = orderJpaEntityRepository.findByNativeSql(sql, OrderJpaEntity.class, "fxtw_fl");

        assertEquals(1, result.size());
        assertSame(expected, result.get(0));
        verify(dataQuery).setParameter(1, "fxtw_fl");
    }

    /** findByHql 分页：COUNT HQL 由 SqlCountSupport 生成，与 HqlQueryBuilder#count() 完全一致 */
    @Test
    public void findByHql_withPageable_usesSqlCountSupportForCountHql() {
        String hql = "SELECT t FROM OrderJpaEntity t WHERE t.province = ?1";
        String countHql = "SELECT COUNT(*) FROM OrderJpaEntity t WHERE t.province = ?1";

        when(entityManager.createQuery(countHql, Long.class)).thenReturn(typedCountQuery);
        when(typedCountQuery.getSingleResult()).thenReturn(25L);
        when(entityManager.createQuery(hql, OrderJpaEntity.class)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(new OrderJpaEntity()));

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderJpaEntity> page = orderJpaEntityRepository.findByHql(hql, pageable, "fxtw_fl");
        System.err.println("-----TotalPages-----:"+page.getTotalPages());

        assertEquals(25L, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        verify(typedCountQuery).setParameter(1, "fxtw_fl");
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(10);
    }

}
