package org.gravitywavetech.extended.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

/**
 * 扩展通用仓储接口。
 *
 * <p>本接口自身不声明任何查询方法，而是通过<b>接口继承</b>组合三类查询片段，
 * 达到「单一职责 + 泛型安全」：</p>
 * <ul>
 *     <li>{@link NativeQueryFragment} —— 动态拼接 Native SQL</li>
 *     <li>{@link HqlQueryFragment} —— 动态拼接 HQL</li>
 *     <li>{@link CriteriaQueryFragment} —— Criteria API</li>
 * </ul>
 *
 * <p>实现由 {@code repositoryBaseClass} = {@link ExtendedBaseRepositoryImpl} 提供
 * （其内部组合委托三个 Fragment 实现类），因此业务侧只需写：</p>
 *
 * <pre>{@code
 * public interface DfApiDictItemRepository extends ExtendedBaseRepository<DfApiDictItem, Long> {
 * }
 * }</pre>
 *
 * <p>即可获得：{@code NativeQueryBuilder<DfApiDictItem> nativeQuery()}、
 * {@code HqlQueryBuilder<DfApiDictItem> hqlQuery()}、
 * {@code CriteriaQueryBuilder<DfApiDictItem> criteriaQuery()} 与
 * {@code Page<DfApiDictItem> findByHql(...)} 等全部带泛型的查询能力，
 * 且可按需重写 {@code save()} 等 CRUD 方法。</p>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author Administrator
 * @version 1.0
 */
@NoRepositoryBean
public interface ExtendedBaseRepository<T, ID extends Serializable>
        extends JpaRepository<T, ID>,
        JpaSpecificationExecutor<T>,
        NativeQueryFragment<T>,
        HqlQueryFragment<T>,
        CriteriaQueryFragment<T> {
}
