package org.gravitywavetech.extended.jpa.repository;

import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.domain.model.OrderStatus;
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
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NativeQueryFragmentImpl（原生 SQL 查询片段）单元测试
 *
 * <p>以 {@code t_order}（订单表）为业务场景，通过 Mockito 模拟
 * {@link EntityManager}，验证：
 * <ol>
 *   <li>{@link NativeQueryBuilder} 针对该表生成的 SQL 字符串与参数绑定是否正确</li>
 *   <li>{@code findByNativeSql} 位置参数绑定与实体结果返回是否正确</li>
 *   <li>{@code findByNativeSql} 分页时 COUNT SQL 构建（子查询包裹）与分页参数是否正确</li>
 *   <li>{@code findByNativeSqlForMap} 结果到 {@code col_N} 的映射是否正确</li>
 *   <li>{@code findByNativeSqlForMap} 分页执行是否正确</li>
 * </ol>
 * </p>
 *
 * <p>对应表结构：</p>
 * <pre>
 * CREATE TABLE `t_order` (
 *   `id`            bigint       NOT NULL COMMENT '主键ID',
 *   `buyer_id`      bigint       NULL COMMENT '买家ID',
 *   `status`        varchar(32)  NULL COMMENT '订单状态',
 *   `total_amount`  decimal(19,2) NULL COMMENT '订单总金额',
 *   `province`      varchar(64)  NULL COMMENT '收货省',
 *   `city`          varchar(64)  NULL COMMENT '收货市',
 *   `detail_address` varchar(256) NULL COMMENT '收货详细地址',
 *   ...
 * );
 * </pre>
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class NativeQueryFragmentImplTest {

    @Mock
    private EntityManager entityManager;

    /** 数据查询（createNativeQuery(sql, resultClass)）返回的 Query */
    @Mock
    private Query query;

    /** 计数查询（createNativeQuery(countSql)）返回的 Query */
    @Mock
    private Query countQuery;

    /** 被测对象：直接构造，不走 Spring 容器。片段实现已参数化，泛型可完整保留 */
    private NativeQueryFragmentImpl<OrderJpaEntity> nativeFragment;

    @BeforeEach
    public void setUp() {
        nativeFragment = new NativeQueryFragmentImpl<>(entityManager, OrderJpaEntity.class);
    }

    /** 片段实现参数化后，直接返回 {@code NativeQueryBuilder<OrderJpaEntity>}，无需强转 */
    private NativeQueryBuilder<OrderJpaEntity> newBuilder() {
        return nativeFragment.nativeQuery();
    }

    /** 构造一条订单数据 */
    private OrderJpaEntity orderEntity(long id, String province, String city) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(id);
        entity.setBuyerId(42L);
        entity.setStatus(OrderStatus.WAITING_PAYMENT);
        entity.setTotalAmount(new BigDecimal("1999.00"));
        entity.setProvince(province);
        entity.setCity(city);
        return entity;
    }

    // ==================== SQL 字符串生成测试 ====================

    /** 演示：select + from(表名) + eq 生成针对 t_order 的查询 */
    @Test
    public void nativeQuery_selectFromEq_generatesCorrectSqlAndParams() {
        NativeQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .select("id", "buyer_id", "province", "city")
                .from("t_order")
                .eq("status", "PAID")
                .eq("buyer_id", 42L);

        String sql = builder.getSql();
        List<Object> params = builder.getParams();

        log.info("生成的原生SQL: {}", sql);
        log.info("绑定参数: {}", params);

        assertEquals("SELECT id, buyer_id, province, city "
                + "FROM t_order WHERE status = ?1 AND buyer_id = ?2 ", sql);
        assertEquals(Arrays.asList("PAID", 42L), params);
    }

    /** 演示：select(*) + LIKE + IN + ORDER BY 组合条件 */
    @Test
    public void nativeQuery_likeAndInAndOrderBy_generatesExpectedSql() {
        NativeQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .select("*")
                .from("t_order")
                .eq("status", "PAID")
                .like("province", "BJ")
                .in("city", Arrays.asList("Haidian", "Chaoyang", "Xicheng"))
                .orderBy("total_amount", "ASC");

        String sql = builder.getSql();
        List<Object> params = builder.getParams();

        log.info("生成的原生SQL: {}", sql);
        log.info("绑定参数: {}", params);

        assertEquals("SELECT * FROM t_order "
                + "WHERE status = ?1 AND province LIKE ?2 AND city IN (?3, ?4, ?5) "
                + "ORDER BY total_amount ASC ", sql);
        assertEquals(Arrays.asList("PAID", "%BJ%", "Haidian", "Chaoyang", "Xicheng"), params);
    }

    /** 演示：eq 传入 null 值时自动跳过条件（动态查询核心特性） */
    @Test
    public void nativeQuery_eqWithNullValue_skipsCondition() {
        NativeQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .select("id")
                .from("t_order")
                .eq("status", null)
                .eq("buyer_id", 42L);

        assertEquals("SELECT id FROM t_order WHERE buyer_id = ?1 ", builder.getSql());
        assertEquals(Collections.singletonList(42L), builder.getParams());
    }

    /** 演示：isNull / isNotNull 条件不产生绑定参数 */
    @Test
    public void nativeQuery_isNullAndIsNotNull_generatesCorrectClauses() {
        NativeQueryBuilder<OrderJpaEntity> builder = newBuilder()
                .select("*")
                .from("t_order")
                .isNull("detail_address")
                .isNotNull("province");

        assertEquals("SELECT * FROM t_order "
                + "WHERE detail_address IS NULL AND province IS NOT NULL ", builder.getSql());
        assertTrue(builder.getParams().isEmpty());
    }

    // ==================== findByNativeSql 测试 ====================

    /** 演示：findByNativeSql 按位置绑定参数并返回实体列表 */
    @Test
    public void findByNativeSql_bindsPositionalParamsAndReturnsEntities() {
        String sql = "SELECT * FROM t_order WHERE buyer_id = ?1 AND status = ?2";
        OrderJpaEntity expect = orderEntity(1L, "BJ", "Haidian");

        when(entityManager.createNativeQuery(sql, OrderJpaEntity.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(expect));

        List<OrderJpaEntity> result = nativeFragment.findByNativeSql(sql, OrderJpaEntity.class, 42L, "PAID");

        assertEquals(1, result.size());
        assertSame(expect, result.get(0));
        verify(entityManager).createNativeQuery(sql, OrderJpaEntity.class);
        verify(query).setParameter(1, 42L);
        verify(query).setParameter(2, "PAID");
        verify(query).getResultList();
    }

    /** 演示：无参数时不进行任何参数绑定 */
    @Test
    public void findByNativeSql_withoutParams_doesNotBindAnyParameter() {
        String sql = "SELECT * FROM t_order WHERE status = 'PAID'";

        when(entityManager.createNativeQuery(sql, OrderJpaEntity.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        List<OrderJpaEntity> result = nativeFragment.findByNativeSql(sql, OrderJpaEntity.class);

        assertTrue(result.isEmpty());
        verify(query, never()).setParameter(anyInt(), any());
    }

    // ==================== findByNativeSql 分页测试 ====================

    /** 演示：分页查询先 COUNT 后取数据；COUNT SQL 由 SqlCountSupport 统一子查询包裹 */
    @Test
    public void findByNativeSql_withPageable_wrapsSubQueryForCountSqlAndPages() {
        String sql = "SELECT * FROM t_order "
                + "WHERE buyer_id = ?1 ORDER BY total_amount ASC";
        // SqlCountSupport.buildCountSql 统一子查询包裹，不做任何字符串解析，
        // 因此原生 SQL 中的 ORDER BY / GROUP BY / JOIN 都不会影响 COUNT 的正确性。
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") t";

        // 注：PageImpl 在 offset + pageSize > total 时会把 total 修正为 offset + contentSize，
        // 这里取 total=12 大于 offset(5)+pageSize(5)，以便直接断言 COUNT 查询返回的总数。
        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(12L);
        when(entityManager.createNativeQuery(sql, OrderJpaEntity.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(orderEntity(1L, "BJ", "Haidian")));

        Pageable pageable = PageRequest.of(1, 5);
        Page<OrderJpaEntity> page = nativeFragment.findByNativeSql(sql, OrderJpaEntity.class, pageable, 42L);

        log.info("分页结果: total={}, contentSize={}", page.getTotalElements(), page.getContent().size());

        assertEquals(12L, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        verify(entityManager).createNativeQuery(countSql);
        verify(entityManager).createNativeQuery(sql, OrderJpaEntity.class);
        verify(countQuery).setParameter(1, 42L);
        verify(query).setParameter(1, 42L);
        verify(query).setFirstResult(5);
        verify(query).setMaxResults(5);
    }

    /** 演示：无 FROM 的 SQL（如 SELECT 1）同样能正确生成 COUNT SQL */
    @Test
    public void findByNativeSql_withPageableAndNoFromClause_wrapsSubQueryForCount() {
        String sql = "SELECT 1";
        String countSql = "SELECT COUNT(*) FROM (SELECT 1) t";

        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(entityManager.createNativeQuery(sql, OrderJpaEntity.class)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderJpaEntity> page = nativeFragment.findByNativeSql(sql, OrderJpaEntity.class, pageable);

        assertEquals(0L, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
        verify(entityManager).createNativeQuery(countSql);
        verify(query).setFirstResult(0);
        verify(query).setMaxResults(10);
    }

    // ==================== findByNativeSqlForMap 测试 ====================

    /** 演示：未指定结果类型时，按列下标映射为 col_0、col_1 ... */
    @Test
    public void findByNativeSqlForMap_mapsColumnsToColIndex() {
        String sql = "SELECT id, province, city FROM t_order "
                + "WHERE buyer_id = ?1 AND status = ?2";

        when(entityManager.createNativeQuery(sql)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(
                new Object[]{1L, "BJ", "Haidian"},
                new Object[]{2L, "SH", "Pudong"}));

        List<Map<String, Object>> result = nativeFragment.findByNativeSqlForMap(sql, 42L, "PAID");

        log.info("Map结果: {}", result);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).get("col_0"));
        assertEquals("BJ", result.get(0).get("col_1"));
        assertEquals("Haidian", result.get(0).get("col_2"));
        assertEquals(2L, result.get(1).get("col_0"));
        assertEquals("Pudong", result.get(1).get("col_2"));
        verify(entityManager).createNativeQuery(sql);
        verify(query).setParameter(1, 42L);
        verify(query).setParameter(2, "PAID");
    }

    /** 演示：Map 形式的分页查询，返回 Page<Map> 并设置分页参数 */
    @Test
    public void findByNativeSqlForMap_withPageable_returnsPageOfMap() {
        String sql = "SELECT id, province FROM t_order WHERE buyer_id = ?1";
        // 同前：COUNT SQL 与 NativeQueryBuilder#count() 共用 SqlCountSupport
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") t";

        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(2L);
        when(entityManager.createNativeQuery(sql)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(
                new Object[]{1L, "BJ"},
                new Object[]{2L, "SH"}));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Map<String, Object>> page = nativeFragment.findByNativeSqlForMap(sql, pageable, 42L);

        assertEquals(2L, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals("BJ", page.getContent().get(0).get("col_1"));
        verify(countQuery).setParameter(1, 42L);
        verify(query).setParameter(1, 42L);
        verify(query).setFirstResult(0);
        verify(query).setMaxResults(10);
    }

    // ==================== nativeQuery() 辅助断言 ====================

    /** 演示：nativeQuery() 返回可用的构建器实例 */
    @Test
    public void nativeQuery_returnsBuilderInstance() {
        assertNotNull(nativeFragment.nativeQuery());
    }
}
