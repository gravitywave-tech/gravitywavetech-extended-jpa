package org.gravitywavetech.framework.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * HQL 查询片段接口。
 *
 * <p>职责单一：只声明 HQL 相关的查询能力。参数化 {@code T} 后，
 * 继承它的仓储接口可直接获得 {@code HqlQueryBuilder<T>} / {@code List<T>} / {@code Page<T>}，
 * 不再返回 {@code ?} 迫使调用方强转。</p>
 *
 * <p>装配方式参见 {@link NativeQueryFragment}。</p>
 *
 * @param <T> 实体类型
 * @author Administrator
 * @version 1.0
 */
public interface HqlQueryFragment<T> {

    /**
     * 创建 HQL 查询构建器
     */
    HqlQueryBuilder<T> hqlQuery();

    /**
     * 执行 HQL 查询
     */
    List<T> findByHql(String hql, Object... params);

    /**
     * 执行 HQL 分页查询
     */
    Page<T> findByHql(String hql, Pageable pageable, Object... params);
}
