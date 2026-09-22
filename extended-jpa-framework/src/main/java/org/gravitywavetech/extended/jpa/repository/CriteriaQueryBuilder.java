package org.gravitywavetech.extended.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


/**
 * BaseEntity
 *
 * <p>
 *     Criteria API 查询构建器
 *     支持动态拼接查询条件，条件存储为构造器函数，彻底解决跨 Query 复用 Predicate 的问题。
 * </p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/8/20 16:09
 */
public class CriteriaQueryBuilder<T> {

    private final EntityManager entityManager;
    private final Class<T> entityClass;
    private final CriteriaBuilder cb;
    private final CriteriaQuery<T> query;
    private final Root<T> root;
    // 核心修复：不再存储绑定到具体 Root 的 Predicate，而是存储可复用的条件构造器
    private final List<BiFunction<CriteriaBuilder, Root<T>, Predicate>> predicateBuilders;
    private final List<BiFunction<CriteriaBuilder, Root<T>, Order>> orderBuilders;
    // 保留外部直接传入的 Predicate（无法跨 Query 复用，count() 时会检测并提示）
    private final List<Predicate> externalPredicates;
    private boolean distinct;

    public CriteriaQueryBuilder(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
        this.cb = entityManager.getCriteriaBuilder();
        this.query = cb.createQuery(entityClass);
        this.root = query.from(entityClass);
        this.predicateBuilders = new ArrayList<>();
        this.orderBuilders = new ArrayList<>();
        this.externalPredicates = new ArrayList<>();
        this.distinct = false;
    }

    // 注意：本类故意不提供 {@code select(String... fields)}。
    // Criteria API 的 {@link CriteriaQuery#select(Selection...)} 只能返回完整实体（即 {@code T}），
    // 无法像 HQL 那样做字段级投影。若强行用 {@code cb.construct(entityClass, ...)} 拼装 DTO 投影，
    // 泛型会被 {@code (Selection<? extends T>) } 强转掩盖，运行时才抛 ClassCastException，
    // 同时会让 {@link #page(Pageable)} 里 {@link #count()} 的总数与 select 出的行数不一致。
    // 需要字段投影请改用 {@link HqlQueryBuilder}。

    /**
     * 设置去重
     */
    public CriteriaQueryBuilder<T> distinct(boolean distinct) {
        this.distinct = distinct;
        query.distinct(distinct);
        return this;
    }

    // ==================== 条件操作方法（改为存储构造器） ====================

    /**
     * 等于条件
     */
    public CriteriaQueryBuilder<T> eq(String field, Object value) {
        if (value != null) {
            predicateBuilders.add((cb, root) -> cb.equal(root.get(field), value));
        }
        return this;
    }

    /**
     * 不等于条件
     */
    public CriteriaQueryBuilder<T> ne(String field, Object value) {
        if (value != null) {
            predicateBuilders.add((cb, root) -> cb.notEqual(root.get(field), value));
        }
        return this;
    }

    /**
     * IN 条件
     */
    public CriteriaQueryBuilder<T> in(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            predicateBuilders.add((cb, root) -> root.get(field).in(values));
        }
        return this;
    }

    /**
     * NOT IN 条件
     */
    public CriteriaQueryBuilder<T> notIn(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            predicateBuilders.add((cb, root) -> cb.not(root.get(field).in(values)));
        }
        return this;
    }

    /**
     * LIKE 条件
     */
    public CriteriaQueryBuilder<T> like(String field, String value) {
        if (value != null && !value.isEmpty()) {
            predicateBuilders.add((cb, root) -> cb.like(root.get(field), "%" + value + "%"));
        }
        return this;
    }

    /**
     * LIKE 条件（自定义模式）
     */
    public CriteriaQueryBuilder<T> like(String field, String value, NativeQueryBuilder.LikeMode mode) {
        if (value != null && !value.isEmpty()) {
            predicateBuilders.add((cb, root) -> cb.like(root.get(field), mode.format(value)));
        }
        return this;
    }

    /**
     * 大于条件
     */
    public CriteriaQueryBuilder<T> gt(String field, Object value) {
        if (value != null) {
            predicateBuilders.add((cb, root) -> cb.greaterThan(root.get(field), (Comparable) value));
        }
        return this;
    }

    /**
     * 大于等于条件
     */
    public CriteriaQueryBuilder<T> ge(String field, Object value) {
        if (value != null) {
            predicateBuilders.add((cb, root) -> cb.greaterThanOrEqualTo(root.get(field), (Comparable) value));
        }
        return this;
    }

    /**
     * 小于条件
     */
    public CriteriaQueryBuilder<T> lt(String field, Object value) {
        if (value != null) {
            predicateBuilders.add((cb, root) -> cb.lessThan(root.get(field), (Comparable) value));
        }
        return this;
    }

    /**
     * 小于等于条件
     */
    public CriteriaQueryBuilder<T> le(String field, Object value) {
        if (value != null) {
            predicateBuilders.add((cb, root) -> cb.lessThanOrEqualTo(root.get(field), (Comparable) value));
        }
        return this;
    }

    /**
     * BETWEEN 条件
     */
    public CriteriaQueryBuilder<T> between(String field, Object start, Object end) {
        if (start != null && end != null) {
            predicateBuilders.add((cb, root) -> cb.between(root.get(field), (Comparable) start, (Comparable) end));
        }
        return this;
    }

    /**
     * IS NULL 条件
     */
    public CriteriaQueryBuilder<T> isNull(String field) {
        predicateBuilders.add((cb, root) -> cb.isNull(root.get(field)));
        return this;
    }

    /**
     * IS NOT NULL 条件
     */
    public CriteriaQueryBuilder<T> isNotNull(String field) {
        predicateBuilders.add((cb, root) -> cb.isNotNull(root.get(field)));
        return this;
    }

    /**
     * 添加外部构造的 Predicate。
     * <p><b>注意：</b>由于 Predicate 绑定了特定的 Root，使用此方法后 {@link #count()} 将不可用，
     * 如需分页统计，请使用本类提供的 eq/like/gt 等内置方法构造条件。</p>
     */
    public CriteriaQueryBuilder<T> predicate(Predicate predicate) {
        if (predicate != null) {
            externalPredicates.add(predicate);
        }
        return this;
    }

    /**
     * 添加 OR 条件组。传入的 Predicate 同样绑定当前 Root，使用后将导致 {@link #count()} 不可用。
     */
    public CriteriaQueryBuilder<T> or(Predicate... predicates) {
        if (predicates != null && predicates.length > 0) {
            externalPredicates.add(cb.or(predicates));
        }
        return this;
    }

    /**
     * 添加 AND 条件组。传入的 Predicate 同样绑定当前 Root，使用后将导致 {@link #count()} 不可用。
     */
    public CriteriaQueryBuilder<T> and(Predicate... predicates) {
        if (predicates != null && predicates.length > 0) {
            externalPredicates.add(cb.and(predicates));
        }
        return this;
    }

    // ==================== 排序方法 ====================

    /**
     * 升序排序
     */
    public CriteriaQueryBuilder<T> asc(String field) {
        orderBuilders.add((cb, root) -> cb.asc(root.get(field)));
        return this;
    }

    /**
     * 降序排序
     */
    public CriteriaQueryBuilder<T> desc(String field) {
        orderBuilders.add((cb, root) -> cb.desc(root.get(field)));
        return this;
    }

    /**
     * 清空排序
     */
    public CriteriaQueryBuilder<T> clearOrders() {
        orderBuilders.clear();
        return this;
    }

    // ==================== 构建方法 ====================

    /**
     * 构建 TypedQuery
     */
    public TypedQuery<T> build() {
        List<Predicate> preds = new ArrayList<>();
        // 1. 通过构造器生成基于当前 root 的 Predicate
        for (BiFunction<CriteriaBuilder, Root<T>, Predicate> builder : predicateBuilders) {
            preds.add(builder.apply(cb, root));
        }
        // 2. 追加外部 Predicate（绑定当前 root）
        preds.addAll(externalPredicates);

        if (!preds.isEmpty()) {
            query.where(cb.and(preds.toArray(new Predicate[0])));
        }

        List<Order> ords = orderBuilders.stream()
                .map(f -> f.apply(cb, root))
                .collect(Collectors.toList());
        if (!ords.isEmpty()) {
            query.orderBy(ords);
        }

        return entityManager.createQuery(query);
    }

    /**
     * 执行查询并返回结果列表
     */
    public List<T> list() {
        return build().getResultList();
    }

    /**
     * 执行分页查询
     */
    public List<T> list(int offset, int limit) {
        TypedQuery<T> typedQuery = build();
        typedQuery.setFirstResult(offset);
        typedQuery.setMaxResults(limit);
        return typedQuery.getResultList();
    }

    /**
     * 执行查询并返回单个结果
     */
    public T single() {
        TypedQuery<T> typedQuery = build();
        typedQuery.setMaxResults(1);
        List<T> results = typedQuery.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 获取 Root 对象
     */
    public Root<T> getRoot() {
        return root;
    }

    /**
     * 获取 CriteriaBuilder
     */
    public CriteriaBuilder getCriteriaBuilder() {
        return cb;
    }

    /**
     * 获取 CriteriaQuery
     */
    public CriteriaQuery<T> getQuery() {
        return query;
    }

    /**
     * 统计总数。
     * 基于当前条件构造器在新的 Count Query 中重新生成 Predicate，彻底避免跨 Root 复用问题。
     *
     * @throws IllegalStateException 如果使用了外部传入的 Predicate（如 {@link #predicate(Predicate)}、{@link #or}、{@link #and}）
     */
    public long count() {
        if (!externalPredicates.isEmpty()) {
            throw new IllegalStateException(
                    "CriteriaQueryBuilder 使用了外部传入的 Predicate（predicate/or/and），" +
                            "这些条件无法自动迁移到 COUNT 查询。请避免使用外部 Predicate，改用内置的 eq/like/gt 等方法构造条件。");
        }

        CriteriaBuilder countCb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = countCb.createQuery(Long.class);
        Root<T> countRoot = countQuery.from(entityClass);

        List<Predicate> preds = predicateBuilders.stream()
                .map(f -> f.apply(countCb, countRoot))
                .collect(Collectors.toList());

        if (!preds.isEmpty()) {
            countQuery.where(countCb.and(preds.toArray(new Predicate[0])));
        }

        countQuery.select(countCb.count(countRoot));
        TypedQuery<Long> q = entityManager.createQuery(countQuery);
        return q.getSingleResult();
    }

    /**
     * 执行分页查询
     */
    public Page<T> page(Pageable pageable) {
        long total = count();
        TypedQuery<T> query = build();
        query.setFirstResult(PageSupport.offsetToInt(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());
        List<T> content = query.getResultList();
        return new PageImpl<>(content, pageable, total);
    }
}