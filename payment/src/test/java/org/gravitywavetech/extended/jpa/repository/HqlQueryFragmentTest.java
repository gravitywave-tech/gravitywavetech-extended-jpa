package org.gravitywavetech.extended.jpa.repository;

import org.gravitywavetech.extended.jpa.demo.entity.PsLawcase;
import org.gravitywavetech.extended.jpa.repository.HqlQueryBuilder;
import org.gravitywavetech.extended.jpa.repository.HqlQueryFragmentImpl;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDateTime;
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
    private TypedQuery<PsLawcase> typedQuery;

    @Mock
    private TypedQuery<Long> countQuery;

    /** 被测对象：直接构造，不走 Spring 容器。片段实现已参数化，泛型可完整保留 */
    private HqlQueryFragmentImpl<PsLawcase> hqlFragment;

    @BeforeEach
    public void setUp() {
        hqlFragment = new HqlQueryFragmentImpl<>(entityManager, PsLawcase.class);
    }

    /** 片段实现参数化后，直接返回 {@code HqlQueryBuilder<PsLawcase>}，无需强转 */
    private HqlQueryBuilder<PsLawcase> newBuilder() {
        return hqlFragment.hqlQuery();
    }

    // ==================== HQL 字符串生成测试 ====================

    /** 演示：select + from + eq + like 的标准用法 */
    @Test
    public void hqlQuery_selectFromEqLike_generatesCorrectHqlAndParams() {
        HqlQueryBuilder<PsLawcase> builder = newBuilder()
                .select("id", "caseno")
                .from()
                .eq("clueId", "DkCHuc")
                .like("caseno", "Nx8sToawnGzgHcuqMH4k7w");

        String hql = builder.getHql();
        List<Object> params = builder.getParams();

        log.info("生成的HQL: {}", hql);
        log.info("绑定参数: {}", params);

        assertEquals("SELECT t.id, t.caseno FROM PsLawcase t WHERE t.clueId = ?1 AND t.caseno LIKE ?2 ", hql);
        assertEquals(Arrays.asList("DkCHuc", "%Nx8sToawnGzgHcuqMH4k7w%"), params);
    }

    /** 演示：eq 传入 null 值时自动跳过条件（动态查询核心特性） */
    @Test
    public void hqlQuery_eqWithNullValue_skipsCondition() {
        HqlQueryBuilder<PsLawcase> builder = newBuilder()
                .from()
                .eq("clueId", null)
                .eq("caseno", "C001");

        assertEquals("FROM PsLawcase t WHERE t.caseno = ?1 ", builder.getHql());
        assertEquals(Collections.singletonList("C001"), builder.getParams());
    }

    /** 演示：IN 条件自动展开为多个位置参数 */
    @Test
    public void hqlQuery_inCondition_generatesInClause() {
        HqlQueryBuilder<PsLawcase> builder = newBuilder()
                .from()
                .in("id", Arrays.asList("1", "2", "3"));

        assertEquals("FROM PsLawcase t WHERE t.id IN (?1, ?2, ?3) ", builder.getHql());
        assertEquals(Arrays.asList("1", "2", "3"), builder.getParams());
    }

    /** 演示：BETWEEN 条件生成两个位置参数 */
    @Test
    public void hqlQuery_betweenCondition_generatesBetweenClause() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 31, 23, 59);

        HqlQueryBuilder<PsLawcase> builder = newBuilder()
                .from()
                .between("createTime", start, end);

        assertEquals("FROM PsLawcase t WHERE t.createTime BETWEEN ?1 AND ?2 ", builder.getHql());
        assertEquals(Arrays.asList(start, end), builder.getParams());
    }

    /** 演示：IS NULL / IS NOT NULL 条件 */
    @Test
    public void hqlQuery_isNullAndIsNotNull_generatesCorrectClauses() {
        HqlQueryBuilder<PsLawcase> builder = newBuilder()
                .from()
                .isNull("deleteStatus")
                .isNotNull("caseno");

        assertEquals("FROM PsLawcase t WHERE t.deleteStatus IS NULL AND t.caseno IS NOT NULL ", builder.getHql());
    }

    /** 演示：ORDER BY 子句 */
    @Test
    public void hqlQuery_orderBy_generatesOrderByClause() {
        HqlQueryBuilder<PsLawcase> builder = newBuilder()
                .from()
                .orderBy("createTime", "DESC");

        assertEquals("FROM PsLawcase t ORDER BY t.createTime DESC ", builder.getHql());
    }

    /** 演示：自定义别名（from("p")），后续条件自动使用该别名前缀 */
    @Test
    public void hqlQuery_fromWithCustomAlias_usesAliasInConditions() {
        HqlQueryBuilder<PsLawcase> builder = newBuilder()
                .from("p")
                .eq("clueId", "C001")
                .like("caseno", "2026");

        assertEquals("FROM PsLawcase p WHERE p.clueId = ?1 AND p.caseno LIKE ?2 ", builder.getHql());
    }

    /** 演示：JOIN 关联查询，字段自动拼接别名前缀 */
    @Test
    public void hqlQuery_join_generatesJoinClause() {
        HqlQueryBuilder<PsLawcase> builder = newBuilder()
                .select("id")
                .from()
                .join("persons")
                .eq("caseno", "C001");

        assertEquals("SELECT t.id FROM PsLawcase t JOIN t.persons WHERE t.caseno = ?1 ", builder.getHql());
        assertEquals(Collections.singletonList("C001"), builder.getParams());
    }

    // ==================== 查询执行测试 ====================

    /** 演示：list() 执行查询，验证 EntityManager 交互 */
    @Test
    public void hqlQuery_list_invokesEntityManagerWithCorrectHqlAndParams() {
        when(entityManager.createQuery(anyString(), eq(PsLawcase.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(new PsLawcase()));

        List<PsLawcase> results = newBuilder()
                .from()
                .eq("clueId", "CLUE-001")
                .list();

        assertEquals(1, results.size());
        // 验证 EntityManager 收到的 HQL（build() 会 trim 尾部空格）
        verify(entityManager).createQuery("FROM PsLawcase t WHERE t.clueId = ?1", PsLawcase.class);
        // 验证位置参数绑定
        verify(typedQuery).setParameter(1, "CLUE-001");
        verify(typedQuery).getResultList();
    }

    /** 演示：page(Pageable) 分页查询，先 COUNT 再查数据 */
    @Test
    public void hqlQuery_page_executesCountAndDataQueries() {
        // 注：PageImpl 在 offset + pageSize > total 时会把 total 修正为 offset + contentSize，
        // 这里取 total=50 大于 offset(0)+pageSize(10)，以便直接断言 COUNT 查询返回的总数。
        when(entityManager.createQuery(contains("SELECT COUNT(*)"), eq(Long.class))).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(50L);
        when(entityManager.createQuery(contains("FROM PsLawcase"), eq(PsLawcase.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(new PsLawcase()));

        Pageable pageable = PageRequest.of(0, 10);
        Page<PsLawcase> page = newBuilder()
                .select("id")
                .from()
                .eq("clueId", "CLUE-001")
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
        PsLawcase expected = new PsLawcase();
        when(entityManager.createQuery(anyString(), eq(PsLawcase.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(expected));

        PsLawcase result = newBuilder()
                .from()
                .eq("id", "001")
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
                .eq("clueId", "CLUE-001")
                .count();

        assertEquals(10L, count);
        verify(entityManager).createQuery("SELECT COUNT(*) FROM PsLawcase t WHERE t.clueId = ?1", Long.class);
        verify(countQuery).setParameter(1, "CLUE-001");
    }

    // ==================== findByHql 测试 ====================

    /** 演示：findByHql 直接执行 HQL 字符串，按位置绑定参数 */
    @Test
    public void findByHql_executesQueryWithPositionalParams() {
        when(entityManager.createQuery(anyString(), eq(PsLawcase.class))).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(new PsLawcase()));

        String hql = "FROM PsLawcase t WHERE t.clueId = ?1 AND t.caseno = ?2";
        List<PsLawcase> results = hqlFragment.findByHql(hql, "CLUE-001", "C001");

        assertEquals(1, results.size());
        verify(entityManager).createQuery(hql, PsLawcase.class);
        verify(typedQuery).setParameter(1, "CLUE-001");
        verify(typedQuery).setParameter(2, "C001");
    }

    /** 演示：findByHql 分页查询，自动构建 COUNT HQL（与 HqlQueryBuilder#count() 共用 SqlCountSupport） */
    @Test
    public void findByHql_withPageable_executesCountAndDataQueries() {
        String hql = "SELECT t FROM PsLawcase t WHERE t.clueId = ?1";
        String countHql = "SELECT COUNT(*) FROM PsLawcase t WHERE t.clueId = ?1";

        when(entityManager.createQuery(countHql, Long.class)).thenReturn(countQuery);
        // total 需大于 offset(0)+pageSize(5)，否则 PageImpl 会修正 total
        when(countQuery.getSingleResult()).thenReturn(30L);
        when(entityManager.createQuery(hql, PsLawcase.class)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.singletonList(new PsLawcase()));

        Pageable pageable = PageRequest.of(0, 5);
        Page<PsLawcase> page = hqlFragment.findByHql(hql, pageable, "CLUE-001");

        assertEquals(30L, page.getTotalElements());
        verify(typedQuery).setParameter(1, "CLUE-001");
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(5);
    }
}
