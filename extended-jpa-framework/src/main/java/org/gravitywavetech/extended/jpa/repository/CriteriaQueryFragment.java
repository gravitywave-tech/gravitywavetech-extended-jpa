package org.gravitywavetech.extended.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Criteria API 查询片段接口。
 *
 * <p>职责单一：只声明 Criteria 相关的查询能力，并保留 {@code T} 泛型。</p>
 *
 * <p>装配方式参见 {@link NativeQueryFragment}。</p>
 *
 * @param <T> 实体类型
 * @author Administrator
 * @version 1.0
 */
public interface CriteriaQueryFragment<T> {

    /**
     * 创建 Criteria API 查询构建器
     */
    CriteriaQueryBuilder<T> criteriaQuery();

    /**
     * 执行 Criteria API 分页查询
     */
    Page<T> findByCriteria(CriteriaQueryBuilder<T> builder, Pageable pageable);
}
