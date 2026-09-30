package org.gravitywavetech.extended.jpa.repository;

import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.infrastructure.repository.jpa.OrderJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HqlQueryFragment（HQL 查询片段）单元测试
 *
 * <p>通过 Mockito 模拟 EntityManager，验证：
 * <ol>
 *   <li>HqlQueryBuilder 生成的 HQL 字符串与参数绑定是否正确</li>
 *   <li>list / page / single / count 执行时与 EntityManager 的交互是否正确</li>
 *   <li>findByHql 位置参数绑定与分页查询是否正确</li>
 * </ol>
 * </p>
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class HqlQueryFragmentTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<OrderJpaEntity> typedQuery;

    @Mock
    private TypedQuery<Long> countQuery;

    /** 被测对象：直接构造，不走 Spring 容器。片段实现已参数化，泛型可完整保留 */
    private HqlQueryFragmentImpl<OrderJpaEntity> hqlFragment;

    @BeforeEach
    public void setUp() {
        hqlFragment = new HqlQueryFragmentImpl<>(entityManager, OrderJpaEntity.class);
    }

    /** 片段实现参数化后，直接返回 {@code HqlQueryBuilder<OrderJpaEntity>}，无需强转 */
    private HqlQueryBuilder<OrderJpaEntity> newBuilder() {
        return hqlFragment.hqlQuery();
    }

    // ==================== HQL 字符串生成测试 ====================

    /** 演示：select + from + eq + like 的标准用法 */
    @Test
    public void hqlQuery_selectFromEqLike_generatesCorrectHqlAndParams() {
        HqlQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .select("id", "province")
                .from()
                .eq("buyerId", 42L)
                .like("province", "BJ");

        String hql = builder.getHql();
        List<Object> params = builder.getParams();

        log.info("生成的HQL: {}", hql);
        log.info("绑定参数: {}", params);

        assertEquals("SELECT t.id, t.province FROM OrderJpaEntity t WHERE t.buyerId = ?1 AND t.province LIKE ?2 ", hql);
        assertEquals(Arrays.asList(42L, "%BJ%"), params);
    }

    /** 演示：eq 传入 null 值时自动跳过条件（动态查询核心特性） */
    @Test
    public void hqlQuery_eqWithNullValue_skipsCondition() {
        HqlQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .from()
                .eq("buyerId", null)
                .eq("province", "BJ");

        assertEquals("FROM OrderJpaEntity t WHERE t.province = ?1 ", builder.getHql());
        assertEquals(Collections.singletonList("BJ"), builder.getParams());
    }

    /** 演示：IN 条件自动展开为多个位置参数 */
    @Test
    public void hqlQuery_inCondition_generatesInClause() {
        HqlQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .from()
                .in("id", Arrays.asList(1L, 2L, 3L));

        assertEquals("FROM OrderJpaEntity t WHERE t.id IN (?1, ?2, ?3) ", builder.getHql());
        assertEquals(Arrays.asList(1L, 2L, 3L), builder.getParams());
    }

    /** 演示：BETWEEN 条件生成两个位置参数 */
    @Test
    public void hqlQuery_betweenCondition_generatesBetweenClause() {
        BigDecimal start = new BigDecimal("100.00");
        BigDecimal end = new BigDecimal("1000.00");

        HqlQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .from()
                .between("totalAmount", start, end);

        assertEquals("FROM OrderJpaEntity t WHERE t.totalAmount BETWEEN ?1 AND ?2 ", builder.getHql());
        assertEquals(Arrays.asList(start, end), builder.getParams());
    }

    /** 演示：IS NULL / IS NOT NULL 条件 */
    @Test
    public void hqlQuery_isNullAndIsNotNull_generatesCorrectClauses() {
        HqlQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .from()
                .isNull("city")
                .isNotNull("province");

        assertEquals("FROM OrderJpaEntity t WHERE t.city IS NULL AND t.province IS NOT NULL ", builder.getHql());
    }

    /** 演示：ORDER BY 子句 */
    @Test
    public void hqlQuery_orderBy_generatesOrderByClause() {
        HqlQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .from()
                .orderBy("totalAmount", "DESC");

        assertEquals("FROM OrderJpaEntity t ORDER BY t.totalAmount DESC ", builder.getHql());
    }

    /** 演示：自定义别名（from("p")），后续条件自动使用该别名前缀 */
    @Test
    public void hqlQuery_fromWithCustomAlias_usesAliasInConditions() {
        HqlQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .from("p")
                .eq("buyerId", 42L)
                .like("province", "BJ");

        assertEquals("FROM OrderJpaEntity p WHERE p.buyerId = ?1 AND p.province LIKE ?2 ", builder.getHql());
    }

    /** 演示：JOIN 关联查询，字段自动拼接别名前缀 */
    @Test
    public void hqlQuery_join_generatesJoinClause() {
        HqlQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .select("id")
                .from()
                .join("items")
                .eq("province", "BJ");

        assertEquals("SELECT t.id FROM OrderJpaEntity t JOIN t.items WHERE t.province = ?1 ", builder.getHql());
        assertEquals(Collections.singletonList("BJ"), builder.getParams());
    }

    // ==================== 查询执行测试 ====================

    /** 演示：list() 执行查询，验证 EntityManager 交互 */
    @Test
    public void hqlQuery_list_invokesEntityManagerWithCorrectHqlAndParams() {
        when(entityManager.createQuery(anyString(), eq(OrderJpaEntity.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(new OrderJpaEntity()));

        List<OrderJpaEntity> results = newBuilder()
                .from()
                .eq("province", "BJ")
                .list();

        assertEquals(1, results.size());
        // 验证 EntityManager 收到的 HQL（build() 会 trim 尾部空格）
        verify(entityManager).createQuery("FROM OrderJpaEntity t WHERE t.province = ?1", OrderJpaEntity.class);
        // 验证位置参数绑定
        verify(typedQuery).setParameter(1, "BJ");
        verify(typedQuery).getResultList();
    }

    /** 演示：page(Pageable) 分页查询，先 COUNT 再查数据 */
    @Test
    public void hqlQuery_page_executesCountAndDataQueries() {
        // 注：PageImpl 在 offset + pageSize > total 时会把 total 修正为 offset + contentSize，
        // 这里取 total=50 大于 offset(0)+pageSize(10)，以便直接断言 COUNT 查询返回的总数。
        when(entityManager.createQuery(contains("SELECT COUNT(*)"), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(50L);
        when(entityManager.createQuery(contains("FROM OrderJpaEntity"), eq(OrderJpaEntity.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(new OrderJpaEntity()));

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderJpaEntity> page = newBuilder()
                .select("id")
                .from()
                .eq("province", "BJ")
                .page(pageable);

        log.info("分页结果: total={}, contentSize={}", page.getTotalElements(), page.getContent().size());

        assertEquals(50L, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(10);
    }

    /** 演示：single() 返回单个结果，内部设置 setMaxResults(1) */
    @Test
    public void hqlQuery_single_returnsFirstResult() {
        OrderJpaEntity expected = new OrderJpaEntity();
        when(entityManager.createQuery(anyString(), eq(OrderJpaEntity.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(expected));

        OrderJpaEntity result = newBuilder()
                .from()
                .eq("id", 100L)
                .single();

        assertSame(expected, result);
        verify(typedQuery).setMaxResults(1);
    }

    /** 演示：count() 自动生成 COUNT 查询 */
    @Test
    public void hqlQuery_count_generatesCountHql() {
        when(entityManager.createQuery(contains("SELECT COUNT(*)"), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(10L);

        long count = newBuilder()
                .select("id")
                .from()
                .eq("province", "BJ")
                .count();

        assertEquals(10L, count);
        verify(entityManager).createQuery("SELECT COUNT(*) FROM OrderJpaEntity t WHERE t.province = ?1", Long.class);
        verify(countQuery).setParameter(1, "BJ");
    }

    // ==================== findByHql 测试 ====================

    /** 演示：findByHql 直接执行 HQL 字符串，按位置绑定参数 */
    @Test
    public void findByHql_executesQueryWithPositionalParams() {
        when(entityManager.createQuery(anyString(), eq(OrderJpaEntity.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(new OrderJpaEntity()));

        String hql = "FROM OrderJpaEntity t WHERE t.id = ?1 AND t.province = ?2";
        List<OrderJpaEntity> results = hqlFragment.findByHql(hql, 100L, "BJ");

        assertEquals(1, results.size());
        verify(entityManager).createQuery(hql, OrderJpaEntity.class);
        verify(typedQuery).setParameter(1, 100L);
        verify(typedQuery).setParameter(2, "BJ");
    }

    /** 演示：findByHql 分页查询，自动构建 COUNT HQL（与 HqlQueryBuilder#count() 共用 SqlCountSupport） */
    @Test
    public void findByHql_withPageable_executesCountAndDataQueries() {
        String hql = "SELECT t FROM OrderJpaEntity t WHERE t.id = ?1";
        String countHql = "SELECT COUNT(*) FROM OrderJpaEntity t WHERE t.id = ?1";

        when(entityManager.createQuery(countHql, Long.class)).thenReturn(countQuery);
        // total 需大于 offset(0)+pageSize(5)，否则 PageImpl 会修正 total
        when(countQuery.getSingleResult()).thenReturn(30L);
        when(entityManager.createQuery(hql, OrderJpaEntity.class)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(new OrderJpaEntity()));

        Pageable pageable = PageRequest.of(0, 5);
        Page<OrderJpaEntity> page = hqlFragment.findByHql(hql, pageable, 100L);

        assertEquals(30L, page.getTotalElements());
        verify(typedQuery).setParameter(1, 100L);
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(5);
    }
}
