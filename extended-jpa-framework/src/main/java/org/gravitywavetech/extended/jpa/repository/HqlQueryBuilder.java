package org.gravitywavetech.extended.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import jakarta.persistence.*;
import java.util.*;
import java.util.stream.Collectors;


/**
 * BaseEntity
 *
 * <p>
 *     HQL 查询构建器
 *     支持动态拼接 WHERE 条件、IN、NOT IN、LIKE 等操作
 * </p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/8/20 16:09
 */
public class HqlQueryBuilder<T> {

    private final EntityManager entityManager;
    private final Class<T> entityClass;
    private final StringBuilder hql;
    private final List<Object> params;
    private final Map<String, Object> namedParams;
    private String countHql;
    private boolean hasWhere = false;
    private String currentAlias = "t";

    public HqlQueryBuilder(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
        this.hql = new StringBuilder();
        this.params = new ArrayList<>();
        this.namedParams = new LinkedHashMap<>();
        this.countHql = null;
    }

    /**
     * 非空字符串校验。用于防止拼出 {@code t.. = ?1} 这类静默错误的 HQL。
     */
    private static String requireText(String value, String what) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(what + " must not be blank");
        }
        return value;
    }

    /**
     * 执行查询并返回 Map 列表
     * 使用 Tuple 方式
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listMap() {
        TypedQuery<Tuple> query = entityManager.createQuery(getHql(), Tuple.class);
        setParameters(query);
        List<Tuple> tuples = query.getResultList();

        return tuples.stream()
                .map(tuple -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    List<TupleElement<?>> elements = tuple.getElements();
                    for (int i = 0; i < elements.size(); i++) {
                        TupleElement<?> element = elements.get(i);
                        String alias = element.getAlias();
                        Object value = tuple.get(i);
                        if (alias != null && !alias.isEmpty()) {
                            map.put(alias, value);
                        } else {
                            map.put("col_" + i, value);
                        }
                    }
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 执行分页查询并返回 Map 列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listMap(int offset, int limit) {
        TypedQuery<Tuple> query = entityManager.createQuery(getHql(), Tuple.class);
        setParameters(query);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        List<Tuple> tuples = query.getResultList();

        return tuples.stream()
                .map(tuple -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    List<TupleElement<?>> elements = tuple.getElements();
                    for (int i = 0; i < elements.size(); i++) {
                        TupleElement<?> element = elements.get(i);
                        String alias = element.getAlias();
                        Object value = tuple.get(i);
                        if (alias != null && !alias.isEmpty()) {
                            map.put(alias, value);
                        } else {
                            map.put("col_" + i, value);
                        }
                    }
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 执行分页查询并返回 Map 列表（使用 Pageable）
     */
    public Page<Map<String, Object>> pageMap(Pageable pageable) {
        long total = count();
        TypedQuery<Tuple> query = entityManager.createQuery(getHql(), Tuple.class);
        setParameters(query);
        query.setFirstResult(PageSupport.offsetToInt(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());

        List<Tuple> tuples = query.getResultList();
        List<Map<String, Object>> content = tuples.stream()
                .map(tuple -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    List<TupleElement<?>> elements = tuple.getElements();
                    for (int i = 0; i < elements.size(); i++) {
                        TupleElement<?> element = elements.get(i);
                        String alias = element.getAlias();
                        Object value = tuple.get(i);
                        if (alias != null && !alias.isEmpty()) {
                            map.put(alias, value);
                        } else {
                            map.put("col_" + i, value);
                        }
                    }
                    return map;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 执行查询并返回 Map 列表（带列名映射）
     * 使用 Object[] 方式
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listMapWithColumns(String... columns) {
        TypedQuery<Object[]> query = entityManager.createQuery(getHql(), Object[].class);
        setParameters(query);
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(arr -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    if (columns != null && columns.length > 0) {
                        for (int i = 0; i < Math.min(columns.length, arr.length); i++) {
                            map.put(columns[i], arr[i]);
                        }
                    } else {
                        for (int i = 0; i < arr.length; i++) {
                            map.put("col_" + i, arr[i]);
                        }
                    }
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * 执行查询并返回单个 Map
     */
    public Map<String, Object> singleMap() {
        TypedQuery<Tuple> query = entityManager.createQuery(getHql(), Tuple.class);
        setParameters(query);
        query.setMaxResults(1);
        List<Tuple> tuples = query.getResultList();
        if (tuples.isEmpty()) {
            return null;
        }
        Tuple tuple = tuples.get(0);
        Map<String, Object> map = new LinkedHashMap<>();
        List<TupleElement<?>> elements = tuple.getElements();
        for (int i = 0; i < elements.size(); i++) {
            TupleElement<?> element = elements.get(i);
            String alias = element.getAlias();
            Object value = tuple.get(i);
            if (alias != null && !alias.isEmpty()) {
                map.put(alias, value);
            } else {
                map.put("col_" + i, value);
            }
        }
        return map;
    }

    /**
     * 构建 COUNT 查询并返回结果
     */
    public Long buildCount() {
        TypedQuery<Long> query = entityManager.createQuery(buildCountHql(), Long.class);
        setParameters(query);
        return query.getSingleResult();
    }

    /**
     * 设置 SELECT（使用当前别名 {@link #from(String)}）
     */
    public HqlQueryBuilder<T> select(String... fields) {
        if (fields == null || fields.length == 0) {
            hql.append("SELECT ").append(currentAlias).append(" ");
        } else {
            hql.append("SELECT ");
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    hql.append(", ");
                }
                hql.append(prefixField(fields[i]));
            }
            hql.append(" ");
        }
        return this;
    }

    /**
     * 设置 SELECT（指定别名）
     */
    public HqlQueryBuilder<T> selectWithAlias(String alias, String... fields) {
        String previous = this.currentAlias;
        this.currentAlias = requireText(alias, "selectWithAlias alias");
        try {
            return select(fields);
        } finally {
            this.currentAlias = previous;
        }
    }

    /**
     * 设置 SELECT 聚合函数 / 表达式，例如 {@code selectExpression("COUNT(t.id)", "total")}
     *
     * <p>本方法不能命名为 {@code select}：否则 {@code select("id", "caseno")} 会被 Java 重载解析
     * 优先匹配到该固定元数方法（固定元数优先于可变参数），本意为「多个字段」的调用
     * 会被当成「表达式 + 别名」，静默生成 {@code SELECT id AS caseno}。</p>
     */
    public HqlQueryBuilder<T> selectExpression(String expression, String alias) {
        hql.append("SELECT ").append(requireText(expression, "selectExpression expression"));
        if (StringUtils.hasText(alias)) {
            hql.append(" AS ").append(alias);
        }
        hql.append(" ");
        return this;
    }

    /**
     * 设置 FROM
     */
    public HqlQueryBuilder<T> from() {
        hql.append("FROM ").append(entityClass.getSimpleName()).append(" t ");
        this.currentAlias = "t";
        return this;
    }

    /**
     * 设置 FROM（带别名）
     */
    public HqlQueryBuilder<T> from(String alias) {
        String a = requireText(alias, "from alias");
        hql.append("FROM ").append(entityClass.getSimpleName()).append(" ").append(a).append(" ");
        this.currentAlias = a;
        return this;
    }

    /**
     * 为字段自动拼接当前别名前缀
     * <p>field 已包含 "."（如 "c.batchFormUsers"）则原样返回，避免重复前缀；
     * 否则用 currentAlias 拼接（如 "batchFormUsers" → "c.batchFormUsers"）</p>
     */
    private String prefixField(String field) {
        if (field == null || field.isEmpty()) {
            return currentAlias + ".";
        }
        if (field.contains(".")) {
            return field;
        }
        return currentAlias + "." + field;
    }

    /**
     * 添加 JOIN
     */
    public HqlQueryBuilder<T> join(String field) {
        hql.append("JOIN ").append(prefixField(requireText(field, "join field"))).append(" ");
        return this;
    }

    /**
     * 添加 JOIN（带别名）
     */
    public HqlQueryBuilder<T> join(String field, String alias) {
        hql.append("JOIN ").append(prefixField(requireText(field, "join field")))
                .append(" ").append(requireText(alias, "join alias")).append(" ");
        return this;
    }

    /**
     * 添加 LEFT JOIN
     */
    public HqlQueryBuilder<T> leftJoin(String field) {
        hql.append("LEFT JOIN ").append(prefixField(requireText(field, "leftJoin field"))).append(" ");
        return this;
    }

    /**
     * 添加 LEFT JOIN（带别名）
     */
    public HqlQueryBuilder<T> leftJoin(String field, String alias) {
        hql.append("LEFT JOIN ").append(prefixField(requireText(field, "leftJoin field")))
                .append(" ").append(requireText(alias, "leftJoin alias")).append(" ");
        return this;
    }

    /**
     * 添加 FETCH JOIN
     */
    public HqlQueryBuilder<T> joinFetch(String field) {
        hql.append("JOIN FETCH ").append(prefixField(requireText(field, "joinFetch field"))).append(" ");
        return this;
    }

    /**
     * 添加 LEFT FETCH JOIN
     */
    public HqlQueryBuilder<T> leftJoinFetch(String field) {
        hql.append("LEFT JOIN FETCH ").append(prefixField(requireText(field, "leftJoinFetch field"))).append(" ");
        return this;
    }

    /**
     * 添加 WHERE 条件（首个条件自动加WHERE，后续自动加AND）
     */
    public HqlQueryBuilder<T> where() {
        if (hasWhere) {
            hql.append("AND ");
        } else {
            hql.append("WHERE ");
            hasWhere = true;
        }
        return this;
    }

    /**
     * 添加 AND
     */
    public HqlQueryBuilder<T> and() {
        hql.append("AND ");
        return this;
    }

    /**
     * 添加 OR
     */
    public HqlQueryBuilder<T> or() {
        hql.append("OR ");
        return this;
    }

    /**
     * 添加左括号（自动加 WHERE/AND）
     */
    public HqlQueryBuilder<T> leftParenthesis() {
        where();
        hql.append("( ");
        return this;
    }

    /**
     * 添加右括号
     */
    public HqlQueryBuilder<T> rightParenthesis() {
        hql.append(") ");
        return this;
    }

    // ==================== 条件操作方法 ====================

    /**
     * 等于条件 = ?（位置参数）
     * 使用当前 from() 设置的别名
     */
    public HqlQueryBuilder<T> eq(String field, Object value) {
        if (value != null) {
            where();
            hql.append(prefixField(requireText(field, "eq field"))).append(" = ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * 等于条件 = ?（指定别名，位置参数）
     */
    public HqlQueryBuilder<T> eqWithAlias(String alias, String field, Object value) {
        if (value != null) {
            eq(requireText(alias, "eqWithAlias alias") + "." + requireText(field, "eqWithAlias field"), value);
        }
        return this;
    }

    /**
     * 等于条件 = :name（命名参数）。
     * <p>与 {@link #eq} 一致：字段如已含别名前缀（如 {@code "c.name"}）则原样使用，
     * 否则自动加上当前 {@link #from(String)} 设定的别名。</p>
     */
    public HqlQueryBuilder<T> eqWithName(String field, String paramName, Object value) {
        if (value != null) {
            where();
            hql.append(prefixField(requireText(field, "eqWithName field")))
                    .append(" = :").append(requireText(paramName, "eqWithName paramName")).append(" ");
            namedParams.put(paramName, value);
        }
        return this;
    }

    /**
     * 不等于条件 != ?
     */
    public HqlQueryBuilder<T> ne(String field, Object value) {
        if (value != null) {
            where();
            hql.append(prefixField(requireText(field, "ne field"))).append(" != ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * IN 条件 IN (?)
     */
    public HqlQueryBuilder<T> in(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            where();
            hql.append(prefixField(requireText(field, "in field"))).append(" IN (");
            int index = 0;
            for (Object value : values) {
                if (index > 0) {
                    hql.append(", ");
                }
                hql.append("?").append(params.size() + 1);
                params.add(value);
                index++;
            }
            hql.append(") ");
        }
        return this;
    }

    /**
     * NOT IN 条件 NOT IN (?)
     */
    public HqlQueryBuilder<T> notIn(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            where();
            hql.append(prefixField(requireText(field, "notIn field"))).append(" NOT IN (");
            int index = 0;
            for (Object value : values) {
                if (index > 0) {
                    hql.append(", ");
                }
                hql.append("?").append(params.size() + 1);
                params.add(value);
                index++;
            }
            hql.append(") ");
        }
        return this;
    }

    /**
     * LIKE 条件 LIKE ?
     */
    public HqlQueryBuilder<T> like(String field, String value) {
        if (StringUtils.hasText(value)) {
            where();
            hql.append(prefixField(requireText(field, "like field"))).append(" LIKE ?").append(params.size() + 1).append(" ");
            params.add("%" + value + "%");
        }
        return this;
    }

    /**
     * LIKE 条件 LIKE ? (自定义通配符)
     */
    public HqlQueryBuilder<T> like(String field, String value, NativeQueryBuilder.LikeMode mode) {
        if (StringUtils.hasText(value)) {
            if (mode == null) {
                throw new IllegalArgumentException("like mode must not be null");
            }
            requireText(field, "like field");
            where();
            hql.append(prefixField(field)).append(" LIKE ?").append(params.size() + 1).append(" ");
            params.add(mode.format(value));
        }
        return this;
    }

    /**
     * 大于条件 > ?
     */
    public HqlQueryBuilder<T> gt(String field, Object value) {
        if (value != null) {
            where();
            hql.append(prefixField(requireText(field, "gt field"))).append(" > ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * 大于等于条件 >= ?
     */
    public HqlQueryBuilder<T> ge(String field, Object value) {
        if (value != null) {
            where();
            hql.append(prefixField(requireText(field, "ge field"))).append(" >= ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * 小于条件 < ?
     */
    public HqlQueryBuilder<T> lt(String field, Object value) {
        if (value != null) {
            where();
            hql.append(prefixField(requireText(field, "lt field"))).append(" < ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * 小于等于条件 <= ?
     */
    public HqlQueryBuilder<T> le(String field, Object value) {
        if (value != null) {
            where();
            hql.append(prefixField(requireText(field, "le field"))).append(" <= ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * BETWEEN 条件 BETWEEN ? AND ?
     */
    public HqlQueryBuilder<T> between(String field, Object start, Object end) {
        if (start != null && end != null) {
            where();
            hql.append(prefixField(requireText(field, "between field"))).append(" BETWEEN ?").append(params.size() + 1);
            params.add(start);
            hql.append(" AND ?").append(params.size() + 1).append(" ");
            params.add(end);
        }
        return this;
    }

    /**
     * IS NULL 条件
     */
    public HqlQueryBuilder<T> isNull(String field) {
        where();
        hql.append(prefixField(requireText(field, "isNull field"))).append(" IS NULL ");
        return this;
    }

    /**
     * IS NOT NULL 条件
     */
    public HqlQueryBuilder<T> isNotNull(String field) {
        where();
        hql.append(prefixField(requireText(field, "isNotNull field"))).append(" IS NOT NULL ");
        return this;
    }

    /**
     * 添加自定义条件，按顺序把 condition 中的 {@code ?} 占位符替换为位置参数并追加对应值。
     * <p>占位符数量必须与 {@code values.length} 一致；不等则抛 {@link IllegalArgumentException}，
     * 避免参数错配导致的静默错误。</p>
     */
    public HqlQueryBuilder<T> appendCondition(String condition, Object... values) {
        if (StringUtils.hasText(condition)) {
            int placeholderCount = countPlaceholders(condition);
            if (values == null) {
                values = new Object[0];
            }
            if (placeholderCount != values.length) {
                throw new IllegalArgumentException(
                        "appendCondition: expected " + placeholderCount
                                + " value(s) to match placeholder(s) in condition, got " + values.length
                                + " — condition=" + condition);
            }
            where();
            StringBuilder processed = new StringBuilder();
            int cursor = 0;
            for (Object value : values) {
                int idx = condition.indexOf('?', cursor);
                if (idx < 0) {
                    break;
                }
                processed.append(condition, cursor, idx);
                processed.append('?').append(params.size() + 1);
                params.add(value);
                cursor = idx + 1;
            }
            processed.append(condition, cursor, condition.length());
            hql.append(processed).append(" ");
        }
        return this;
    }

    private static int countPlaceholders(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    /**
     * 追加原始 HQL 片段。
     * <p><b>警告：</b>此方法直接拼接字符串，若传入外部不可信参数存在 HQL/SQL 注入风险，
     * 请仅用于拼接字段名、JOIN 条件等受控片段；值一律走 {@code eq/like/in} 等参数化方法。</p>
     */
    public HqlQueryBuilder<T> append(String fragment) {
        if (StringUtils.hasText(fragment)) {
            hql.append(fragment).append(" ");
        }
        return this;
    }

    /**
     * 设置 GROUP BY（使用当前 {@link #from(String)} 设置的别名）
     */
    public HqlQueryBuilder<T> groupBy(String... fields) {
        if (fields != null && fields.length > 0) {
            hql.append("GROUP BY ");
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    hql.append(", ");
                }
                hql.append(prefixField(requireText(fields[i], "groupBy field[" + i + "]")));
            }
            hql.append(" ");
        }
        return this;
    }

    /**
     * 设置 HAVING
     */
    public HqlQueryBuilder<T> having(String condition) {
        if (StringUtils.hasText(condition)) {
            hql.append("HAVING ").append(condition).append(" ");
        }
        return this;
    }

    /**
     * 设置 ORDER BY（使用当前 {@link #from(String)} 设置的别名）
     */
    public HqlQueryBuilder<T> orderBy(String field, String direction) {
        String f = requireText(field, "orderBy field");
        String d = requireText(direction, "orderBy direction").toUpperCase();
        if (!d.equals("ASC") && !d.equals("DESC")) {
            throw new IllegalArgumentException("orderBy direction must be ASC or DESC, got: " + direction);
        }
        hql.append("ORDER BY ").append(prefixField(f)).append(" ").append(d).append(" ");
        return this;
    }

    /**
     * 设置 ORDER BY（指定别名）
     */
    public HqlQueryBuilder<T> orderByWithAlias(String alias, String field, String direction) {
        return orderBy(alias + "." + field, direction);
    }

    /**
     * 设置自定义 COUNT HQL
     */
    public HqlQueryBuilder<T> countHql(String countHql) {
        this.countHql = countHql;
        return this;
    }

    // ==================== 构建和执行方法 ====================

    /**
     * 构建 TypedQuery 对象
     */
    public TypedQuery<T> build() {
        String hqlStr = hql.toString().trim();
        TypedQuery<T> query = entityManager.createQuery(hqlStr, entityClass);
        setParameters(query);
        return query;
    }

    /**
     * 构建 TypedQuery 对象（指定结果类型）
     */
    public <R> TypedQuery<R> build(Class<R> resultClass) {
        String hqlStr = hql.toString().trim();
        TypedQuery<R> query = entityManager.createQuery(hqlStr, resultClass);
        setParameters(query);
        return query;
    }

    /**
     * 设置参数
     */
    private void setParameters(Query query) {
        // 设置位置参数
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }
        // 设置命名参数
        for (Map.Entry<String, Object> entry : namedParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 执行查询并返回结果列表
     */
    public List<T> list() {
        return build().getResultList();
    }

    /**
     * 执行查询并返回结果列表（指定结果类型）
     */
    public <R> List<R> list(Class<R> resultClass) {
        return build(resultClass).getResultList();
    }

    /**
     * 执行分页查询
     */
    public List<T> page(int offset, int limit) {
        TypedQuery<T> query = build();
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    /**
     * 执行分页查询（指定结果类型）
     */
    public <R> List<R> page(int offset, int limit, Class<R> resultClass) {
        TypedQuery<R> query = build(resultClass);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    /**
     * 执行分页查询（使用 Pageable）
     */
    public Page<T> page(Pageable pageable) {
        return page(pageable, entityClass);
    }

    /**
     * 执行分页查询（使用 Pageable，指定结果类型）
     */
    public <R> Page<R> page(Pageable pageable, Class<R> resultClass) {
        // 1. 查询总数
        long total = count();

        // 2. 查询分页数据
        TypedQuery<R> query = build(resultClass);
        query.setFirstResult(PageSupport.offsetToInt(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());

        List<R> content = query.getResultList();
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 统计总数
     */
    public long count() {
        String countHqlStr = buildCountHql();
        TypedQuery<Long> countQuery = entityManager.createQuery(countHqlStr, Long.class);
        setParameters(countQuery);
        return countQuery.getSingleResult();
    }

    /**
     * 构建 COUNT HQL。
     *
     * <p>统一委托 {@link SqlCountSupport}，与 HQL 查询片段共用同一份规则：
     * 简单 HQL 安全剥离 SELECT / ORDER BY；复杂 HQL（含 GROUP BY / UNION / 子查询）
     * 要求调用方显式设置 countHql。</p>
     */
    private String buildCountHql() {
        if (countHql != null && !countHql.isEmpty()) {
            return countHql;
        }
        return SqlCountSupport.buildCountHql(hql.toString());
    }

//    /**
//     * 构建 COUNT HQL
//     * 智能处理 SELECT new DTO(...) / 聚合函数 / GROUP BY 场景
//     */
//    private String buildCountHql() {
//        if (countHql != null && !countHql.isEmpty()) {
//            return countHql;
//        }
//
//        String hqlStr = hql.toString().trim();
//
//        // 如果使用了 GROUP BY，HQL不支持子查询包装，需改写为 COUNT(DISTINCT 主键) 形式
//        // 但由于无法自动识别主键，这里返回主查询的聚合结果计数（需调用方自定义countHql）
//        if (hasGroupBy) {
//            // 尝试提取 GROUP BY 后的字段，构建 COUNT(DISTINCT ...)
//            String upperHql = hqlStr.toUpperCase();
//            int groupByIndex = upperHql.lastIndexOf(" GROUP BY ");
//            if (groupByIndex != -1) {
//                String groupByFields = hqlStr.substring(groupByIndex + " GROUP BY ".length());
//                // 去除 ORDER BY 部分
//                int orderByIdx = groupByFields.toUpperCase().indexOf(" ORDER BY ");
//                if (orderByIdx != -1) {
//                    groupByFields = groupByFields.substring(0, orderByIdx);
//                }
//                // 剥离 SELECT 部分，保留 FROM 到 GROUP BY 前
//                int fromIdx = upperHql.indexOf(" FROM ");
//                String fromToGroupBy = hqlStr.substring(fromIdx, groupByIndex);
//                return "SELECT COUNT(DISTINCT " + groupByFields.trim() + ") " + fromToGroupBy;
//            }
//            // 兜底：返回主查询（调用方需自定义countHql）
//            return hqlStr;
//        }
//
//        String upperHql = hqlStr.toUpperCase();
//        int fromIndex = upperHql.indexOf(" FROM ");
//        if (fromIndex == -1) {
//            return hqlStr;
//        }
//
//        // 处理 ORDER BY（COUNT 查询不需要 ORDER BY）
//        int orderByIndex = upperHql.lastIndexOf(" ORDER BY ");
//        String baseHql = hqlStr;
//        if (orderByIndex != -1) {
//            baseHql = hqlStr.substring(0, orderByIndex);
//            upperHql = baseHql.toUpperCase();
//            fromIndex = upperHql.indexOf(" FROM ");
//        }
//
//        // 智能剥离 SELECT 部分：
//        // 1. SELECT new DTO(...) → SELECT COUNT(*)
//        // 2. SELECT 聚合函数 → SELECT COUNT(*)
//        // 3. SELECT t.field → SELECT COUNT(*)
//        // 4. SELECT t（无字段） → SELECT COUNT(*)
//        return "SELECT COUNT(*) " + baseHql.substring(fromIndex);
//    }

    /**
     * 执行查询并返回单个结果
     */
    public T single() {
        TypedQuery<T> query = build();
        query.setMaxResults(1);
        List<T> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 执行查询并返回单个结果（指定结果类型）
     */
    public <R> R single(Class<R> resultClass) {
        TypedQuery<R> query = build(resultClass);
        query.setMaxResults(1);
        List<R> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 获取构建的 HQL
     */
    public String getHql() {
        return hql.toString();
    }

    /**
     * 获取位置参数列表
     */
    public List<Object> getParams() {
        return new ArrayList<>(params);
    }

    /**
     * 获取命名参数
     */
    public Map<String, Object> getNamedParams() {
        return new LinkedHashMap<>(namedParams);
    }

    /**
     * 清空条件（保留 SELECT 和 FROM）
     */
    public HqlQueryBuilder<T> clearConditions() {
        String hqlStr = hql.toString();
        int whereIndex = hqlStr.toUpperCase().indexOf(" WHERE ");
        if (whereIndex != -1) {
            hql.setLength(whereIndex);
        }
        params.clear();
        namedParams.clear();
        hasWhere = false;
        return this;
    }

    /**
     * LIKE 条件 LIKE ?（指定别名）
     */
    public HqlQueryBuilder<T> likeWithAlias(String alias, String field, String value) {
        if (StringUtils.hasText(value)) {
            like(alias + "." + field, value);
        }
        return this;
    }

    /**
     * LIKE 条件 LIKE ?（指定别名，自定义通配符）
     */
    public HqlQueryBuilder<T> likeWithAlias(String alias, String field, String value, NativeQueryBuilder.LikeMode mode) {
        if (StringUtils.hasText(value)) {
            like(alias + "." + field, value, mode);
        }
        return this;
    }

    /**
     * 大于条件 > ?（指定别名）
     */
    public HqlQueryBuilder<T> gtWithAlias(String alias, String field, Object value) {
        if (value != null) gt(alias + "." + field, value);
        return this;
    }

    /**
     * 大于等于条件 >= ?（指定别名）
     */
    public HqlQueryBuilder<T> geWithAlias(String alias, String field, Object value) {
        if (value != null) ge(alias + "." + field, value);
        return this;
    }

    /**
     * 小于条件 < ?（指定别名）
     */
    public HqlQueryBuilder<T> ltWithAlias(String alias, String field, Object value) {
        if (value != null) lt(alias + "." + field, value);
        return this;
    }

    /**
     * 小于等于条件 <= ?（指定别名）
     */
    public HqlQueryBuilder<T> leWithAlias(String alias, String field, Object value) {
        if (value != null) le(alias + "." + field, value);
        return this;
    }

    /**
     * IN 条件 IN (?)（指定别名）
     */
    public HqlQueryBuilder<T> inWithAlias(String alias, String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) in(alias + "." + field, values);
        return this;
    }

    /**
     * NOT IN 条件 NOT IN (?)（指定别名）
     */
    public HqlQueryBuilder<T> notInWithAlias(String alias, String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) notIn(alias + "." + field, values);
        return this;
    }

    /**
     * BETWEEN 条件 BETWEEN ? AND ?（指定别名）
     */
    public HqlQueryBuilder<T> betweenWithAlias(String alias, String field, Object start, Object end) {
        if (start != null && end != null) between(alias + "." + field, start, end);
        return this;
    }

    /**
     * IS NULL 条件（指定别名）
     */
    public HqlQueryBuilder<T> isNullWithAlias(String alias, String field) {
        return isNull(alias + "." + field);
    }

    /**
     * IS NOT NULL 条件（指定别名）
     */
    public HqlQueryBuilder<T> isNotNullWithAlias(String alias, String field) {
        return isNotNull(alias + "." + field);
    }
}