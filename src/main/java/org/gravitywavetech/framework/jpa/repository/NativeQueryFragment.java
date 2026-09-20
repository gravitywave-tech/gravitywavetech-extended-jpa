package org.gravitywavetech.framework.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

/**
 * 原生 SQL 查询片段接口。
 *
 * <p>职责单一：只声明 Native SQL 相关的查询能力。参数化 {@code T} 后，
 * 继承它的仓储接口可以直接获得 {@code NativeQueryBuilder<T>}，
 * 无需再对返回值做强转（这是 Fragment 拆分方案此前最大的缺陷）。</p>
 *
 * <p>装配方式有两种，按需选择：</p>
 * <ol>
 *     <li>仓储接口继承 {@link ExtendedBaseRepository}（推荐）：由 {@code repositoryBaseClass}
 *         {@link ExtendedBaseRepositoryImpl} 组合委托 {@link NativeQueryFragmentImpl} 提供实现，
 *         泛型与 CRUD 覆盖能力都保留。</li>
 *     <li>仓储接口直接 {@code extends JpaRepository<T, ID>, NativeQueryFragment<T>}：
 *         由 {@link ExtendedJpaRepositoryFactory} 在代理创建时注入实现，实现按需装配。</li>
 * </ol>
 *
 * @param <T> 实体类型
 * @author Administrator
 * @version 1.0
 */
public interface NativeQueryFragment<T> {

    /**
     * 创建 Native SQL 查询构建器
     */
    NativeQueryBuilder<T> nativeQuery();

    /**
     * 执行 Native SQL 查询
     */
    <R> List<R> findByNativeSql(String sql, Class<R> resultClass, Object... params);

    /**
     * 执行 Native SQL 分页查询
     */
    <R> Page<R> findByNativeSql(String sql, Class<R> resultClass, Pageable pageable, Object... params);

    /**
     * 执行 Native SQL 查询，返回 Map 列表
     */
    List<Map<String, Object>> findByNativeSqlForMap(String sql, Object... params);

    /**
     * 执行 Native SQL 分页查询，返回 Map 列表
     */
    Page<Map<String, Object>> findByNativeSqlForMap(String sql, Pageable pageable, Object... params);
}
