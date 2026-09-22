package org.gravitywavetech.extended.jpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import java.util.*;
import java.util.stream.Collectors;


/**
 * BaseEntity
 *
 * <p>
 *     Native SQL 查询构建器
 *     支支持动态拼接 WHERE 条件、IN、NOT IN、LIKE 等操作
 * </p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/8/20 16:09
 */
public class NativeQueryBuilder<T> {

    private final EntityManager entityManager;
    private final Class<T> entityClass;
    private final StringBuilder sql;
    private final List<Object> params;
    private final List<String> paramNames;
    private String countSql;
    private boolean hasGroupBy = false;
    private boolean hasWhere = false;
    // 分页参数由 JPA setFirstResult/setMaxResults 处理，不再拼接到 SQL
    private Integer limitOffset;
    private Integer limitRows;

    public NativeQueryBuilder(EntityManager entityManager, Class<T> entityClass) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
        this.sql = new StringBuilder();
        this.params = new ArrayList<>();
        this.paramNames = new ArrayList<>();
        this.countSql = null;
    }

    /**
     * 非空字符串校验。用于防止拼出 {@code . = ?1} 这类静默错误的 SQL。
     */
    private static String requireText(String value, String what) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(what + " must not be blank");
        }
        return value;
    }

    /**
     * 设置 SELECT 字段
     */
    public NativeQueryBuilder<T> select(String... fields) {
        if (fields == null || fields.length == 0) {
            sql.append("SELECT * ");
        } else {
            sql.append("SELECT ").append(String.join(", ", fields)).append(" ");
        }
        return this;
    }

    /**
     * 设置 SELECT 字段（带别名）
     */
    public NativeQueryBuilder<T> select(Map<String, String> fieldAliasMap) {
        if (fieldAliasMap != null && !fieldAliasMap.isEmpty()) {
            sql.append("SELECT ");
            List<String> selectItems = new ArrayList<>();
            for (Map.Entry<String, String> entry : fieldAliasMap.entrySet()) {
                selectItems.add(entry.getKey() + " AS " + entry.getValue());
            }
            sql.append(String.join(", ", selectItems)).append(" ");
        }
        return this;
    }

    /**
     * 设置 FROM 表
     */
    public NativeQueryBuilder<T> from(String tableName) {
        sql.append("FROM ").append(requireText(tableName, "from tableName")).append(" ");
        return this;
    }

    /**
     * 设置 FROM 表（自动使用实体类名）
     */
    public NativeQueryBuilder<T> from() {
        String tableName = entityClass.getSimpleName();
        return from(tableName);
    }

    /**
     * 设置 FROM 表（带别名）
     */
    public NativeQueryBuilder<T> from(String tableName, String alias) {
        sql.append("FROM ").append(requireText(tableName, "from tableName"))
                .append(" ").append(requireText(alias, "from alias")).append(" ");
        return this;
    }

    /**
     * 添加 JOIN
     */
    public NativeQueryBuilder<T> join(String joinSql) {
        sql.append(requireText(joinSql, "join joinSql")).append(" ");
        return this;
    }

    /**
     * 添加 LEFT JOIN
     */
    public NativeQueryBuilder<T> leftJoin(String tableName, String alias, String onCondition) {
        sql.append("LEFT JOIN ").append(requireText(tableName, "leftJoin tableName"))
                .append(" ").append(requireText(alias, "leftJoin alias"))
                .append(" ON ").append(requireText(onCondition, "leftJoin onCondition")).append(" ");
        return this;
    }

    /**
     * 添加 INNER JOIN
     */
    public NativeQueryBuilder<T> innerJoin(String tableName, String alias, String onCondition) {
        sql.append("INNER JOIN ").append(requireText(tableName, "innerJoin tableName"))
                .append(" ").append(requireText(alias, "innerJoin alias"))
                .append(" ON ").append(requireText(onCondition, "innerJoin onCondition")).append(" ");
        return this;
    }

    /**
     * 添加 WHERE 条件（首个条件自动加WHERE，后续自动加AND）
     */
    public NativeQueryBuilder<T> where() {
        if (hasWhere) {
            sql.append("AND ");
        } else {
            sql.append("WHERE ");
            hasWhere = true;
        }
        return this;
    }

    /**
     * 添加 AND 条件
     */
    public NativeQueryBuilder<T> and() {
        sql.append("AND ");
        return this;
    }

    /**
     * 添加 OR 条件
     */
    public NativeQueryBuilder<T> or() {
        sql.append("OR ");
        return this;
    }

    /**
     * 添加左括号（自动加 WHERE/AND）
     */
    public NativeQueryBuilder<T> leftParenthesis() {
        where();
        sql.append("( ");
        return this;
    }

    /**
     * 添加右括号
     */
    public NativeQueryBuilder<T> rightParenthesis() {
        sql.append(") ");
        return this;
    }

    // ==================== 条件操作方法 ====================

    /**
     * 等于条件 = ?
     */
    public NativeQueryBuilder<T> eq(String field, Object value) {
        if (value != null) {
            where();
            sql.append(requireText(field, "eq field")).append(" = ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * 等于条件 = ?（带别名）
     */
    public NativeQueryBuilder<T> eqWithAlias(String alias, String field, Object value) {
        if (value != null) {
            where();
            sql.append(requireText(alias, "eqWithAlias alias")).append(".")
                    .append(requireText(field, "eqWithAlias field"))
                    .append(" = ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * 不等于条件 != ?
     */
    public NativeQueryBuilder<T> ne(String field, Object value) {
        if (value != null) {
            where();
            sql.append(requireText(field, "ne field")).append(" != ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * IN 条件 IN (?)
     */
    public NativeQueryBuilder<T> in(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            where();
            sql.append(requireText(field, "in field")).append(" IN (");
            int index = 0;
            for (Object value : values) {
                if (index > 0) {
                    sql.append(", ");
                }
                sql.append("?").append(params.size() + 1);
                params.add(value);
                index++;
            }
            sql.append(") ");
        }
        return this;
    }

    /**
     * IN 条件 IN (?)（带别名）
     */
    public NativeQueryBuilder<T> inWithAlias(String alias, String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            where();
            sql.append(requireText(alias, "inWithAlias alias")).append(".")
                    .append(requireText(field, "inWithAlias field")).append(" IN (");
            int index = 0;
            for (Object value : values) {
                if (index > 0) {
                    sql.append(", ");
                }
                sql.append("?").append(params.size() + 1);
                params.add(value);
                index++;
            }
            sql.append(") ");
        }
        return this;
    }

    /**
     * NOT IN 条件 NOT IN (?)
     */
    public NativeQueryBuilder<T> notIn(String field, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            where();
            sql.append(requireText(field, "notIn field")).append(" NOT IN (");
            int index = 0;
            for (Object value : values) {
                if (index > 0) {
                    sql.append(", ");
                }
                sql.append("?").append(params.size() + 1);
                params.add(value);
                index++;
            }
            sql.append(") ");
        }
        return this;
    }

    /**
     * LIKE 条件 LIKE ?
     */
    public NativeQueryBuilder<T> like(String field, String value) {
        if (StringUtils.hasText(value)) {
            where();
            sql.append(requireText(field, "like field")).append(" LIKE ?").append(params.size() + 1).append(" ");
            params.add("%" + value + "%");
        }
        return this;
    }

    /**
     * LIKE 条件 LIKE ?（带别名）
     */
    public NativeQueryBuilder<T> likeWithAlias(String alias, String field, String value) {
        if (StringUtils.hasText(value)) {
            where();
            sql.append(requireText(alias, "likeWithAlias alias")).append(".")
                    .append(requireText(field, "likeWithAlias field"))
                    .append(" LIKE ?").append(params.size() + 1).append(" ");
            params.add("%" + value + "%");
        }
        return this;
    }

    /**
     * LIKE 条件 LIKE ? (自定义通配符)
     */
    public NativeQueryBuilder<T> like(String field, String value, LikeMode mode) {
        if (StringUtils.hasText(value)) {
            if (mode == null) throw new IllegalArgumentException("like mode must not be null");
            where();
            sql.append(requireText(field, "like field")).append(" LIKE ?").append(params.size() + 1).append(" ");
            String pattern = mode.format(value);
            params.add(pattern);
        }
        return this;
    }

    /**
     * LIKE 条件（自定义模式，带别名）
     */
    public NativeQueryBuilder<T> likeWithAlias(String alias, String field, String value, LikeMode mode) {
        if (StringUtils.hasText(value)) {
            if (mode == null) throw new IllegalArgumentException("like mode must not be null");
            where();
            sql.append(requireText(alias, "likeWithAlias alias")).append(".")
                    .append(requireText(field, "likeWithAlias field"))
                    .append(" LIKE ?").append(params.size() + 1).append(" ");
            params.add(mode.format(value));
        }
        return this;
    }

    /**
     * 大于条件 > ?
     */
    public NativeQueryBuilder<T> gt(String field, Object value) {
        if (value != null) {
            where();
            sql.append(requireText(field, "gt field")).append(" > ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * 大于等于条件 >= ?
     */
    public NativeQueryBuilder<T> ge(String field, Object value) {
        if (value != null) {
            where();
            sql.append(requireText(field, "ge field")).append(" >= ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * 小于条件 < ?
     */
    public NativeQueryBuilder<T> lt(String field, Object value) {
        if (value != null) {
            where();
            sql.append(requireText(field, "lt field")).append(" < ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * 小于等于条件 <= ?
     */
    public NativeQueryBuilder<T> le(String field, Object value) {
        if (value != null) {
            where();
            sql.append(requireText(field, "le field")).append(" <= ?").append(params.size() + 1).append(" ");
            params.add(value);
        }
        return this;
    }

    /**
     * BETWEEN 条件 BETWEEN ? AND ?
     */
    public NativeQueryBuilder<T> between(String field, Object start, Object end) {
        if (start != null && end != null) {
            where();
            sql.append(requireText(field, "between field")).append(" BETWEEN ?").append(params.size() + 1);
            params.add(start);
            sql.append(" AND ?").append(params.size() + 1).append(" ");
            params.add(end);
        }
        return this;
    }

    /**
     * IS NULL 条件
     */
    public NativeQueryBuilder<T> isNull(String field) {
        where();
        sql.append(requireText(field, "isNull field")).append(" IS NULL ");
        return this;
    }

    /**
     * IS NOT NULL 条件
     */
    public NativeQueryBuilder<T> isNotNull(String field) {
        where();
        sql.append(requireText(field, "isNotNull field")).append(" IS NOT NULL ");
        return this;
    }

    /**
     * 添加自定义条件，按顺序把 condition 中的 {@code ?} 占位符替换为位置参数并追加对应值。
     * <p>占位符数量必须与 {@code values.length} 一致；不等则抛 {@link IllegalArgumentException}，
     * 避免参数错配导致的静默错误。</p>
     */
    public NativeQueryBuilder<T> appendCondition(String condition, Object... values) {
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
            sql.append(processed).append(" ");
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
     * 追加原始 SQL 片段。
     * <p><b>警告：</b>此方法直接拼接字符串，若传入外部不可信参数存在 SQL 注入风险，
     * 请仅用于拼接表名、列名、JOIN 条件等受控片段。</p>
     */
    public NativeQueryBuilder<T> append(String fragment) {
        if (StringUtils.hasText(fragment)) {
            sql.append(fragment).append(" ");
        }
        return this;
    }

    /**
     * 设置 GROUP BY
     */
    public NativeQueryBuilder<T> groupBy(String... fields) {
        if (fields != null && fields.length > 0) {
            hasGroupBy = true;
            sql.append("GROUP BY ").append(String.join(", ", fields)).append(" ");
        }
        return this;
    }

    /**
     * 设置 HAVING
     */
    public NativeQueryBuilder<T> having(String condition) {
        if (StringUtils.hasText(condition)) {
            sql.append("HAVING ").append(condition).append(" ");
        }
        return this;
    }

    /**
     * 设置 ORDER BY
     */
    public NativeQueryBuilder<T> orderBy(String field, String direction) {
        String f = requireText(field, "orderBy field");
        String d = requireText(direction, "orderBy direction").toUpperCase();
        if (!d.equals("ASC") && !d.equals("DESC")) {
            throw new IllegalArgumentException("orderBy direction must be ASC or DESC, got: " + direction);
        }
        sql.append("ORDER BY ").append(f).append(" ").append(d).append(" ");
        return this;
    }

    /**
     * 设置 ORDER BY（带别名）
     */
    public NativeQueryBuilder<T> orderByWithAlias(String alias, String field, String direction) {
        return orderBy(requireText(alias, "orderByWithAlias alias") + "." + requireText(field, "orderByWithAlias field"), direction);
    }

    /**
     * 设置分页参数。不再拼接 LIMIT 到 SQL，而是通过 JPA 的 setFirstResult/setMaxResults
     * 由 Hibernate 自动翻译为当前数据库方言的分页语法（MySQL、Oracle、PostgreSQL 等）。
     */
    public NativeQueryBuilder<T> limit(int offset, int limit) {
        this.limitOffset = offset;
        this.limitRows = limit;
        return this;
    }

    /**
     * 设置自定义 COUNT SQL
     */
    public NativeQueryBuilder<T> countSql(String countSql) {
        this.countSql = countSql;
        return this;
    }

    // ==================== 构建和执行方法 ====================

    /**
     * 构建 Query 对象
     */
    public Query build() {
        String sqlStr = sql.toString().trim();
        Query query = entityManager.createNativeQuery(sqlStr);
        setParameters(query);
        if (limitOffset != null && limitRows != null) {
            query.setFirstResult(limitOffset);
            query.setMaxResults(limitRows);
        }
        return query;
    }

    /**
     * 构建 Query 对象（指定结果类型）
     */
    public Query build(Class<?> resultClass) {
        String sqlStr = sql.toString().trim();
        Query query = entityManager.createNativeQuery(sqlStr, resultClass);
        setParameters(query);
        if (limitOffset != null && limitRows != null) {
            query.setFirstResult(limitOffset);
            query.setMaxResults(limitRows);
        }
        return query;
    }

    /**
     * 设置参数
     */
    private void setParameters(Query query) {
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }
    }

    /**
     * 执行查询并返回结果列表
     */
    @SuppressWarnings("unchecked")
    public List<T> list() {
        return build().getResultList();
    }

    /**
     * 执行查询并返回结果列表（指定结果类型）
     */
    @SuppressWarnings("unchecked")
    public <R> List<R> list(Class<R> resultClass) {
        return build(resultClass).getResultList();
    }

    /**
     * 执行分页查询
     */
    @SuppressWarnings("unchecked")
    public List<T> page(int offset, int limit) {
        Query query = build();
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    /**
     * 执行分页查询（指定结果类型）
     */
    @SuppressWarnings("unchecked")
    public <R> List<R> page(int offset, int limit, Class<R> resultClass) {
        Query query = build(resultClass);
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
    @SuppressWarnings("unchecked")
    public <R> Page<R> page(Pageable pageable, Class<R> resultClass) {
        // 1. 查询总数
        long total = count();

        // 2. 查询分页数据
        Query query = build(resultClass);
        query.setFirstResult(PageSupport.offsetToInt(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());

        List<R> content = query.getResultList();
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 统计总数。采用子查询包装，彻底避免字符串解析风险，兼容任意复杂 SQL。
     */
    public long count() {
        String countSqlStr = buildCountSql();
        Query countQuery = entityManager.createNativeQuery(countSqlStr);
        setParameters(countQuery);
        return ((Number) countQuery.getSingleResult()).longValue();
    }

    /**
     * 构建 COUNT SQL。统一委托 {@link SqlCountSupport}，与查询片段共用同一份规则。
     */
    private String buildCountSql() {
        if (countSql != null && !countSql.isEmpty()) {
            return countSql;
        }
        return SqlCountSupport.buildCountSql(sql.toString());
    }

    /**
     * 执行查询并返回单个结果
     */
    @SuppressWarnings("unchecked")
    public T single() {
        Query query = build();
        query.setMaxResults(1);
        List<T> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 执行查询并返回单个结果（指定结果类型）
     */
    @SuppressWarnings("unchecked")
    public <R> R single(Class<R> resultClass) {
        Query query = build(resultClass);
        query.setMaxResults(1);
        List<R> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 执行更新操作
     */
    public int executeUpdate() {
        return build().executeUpdate();
    }

    /**
     * 获取构建的 SQL
     */
    public String getSql() {
        return sql.toString();
    }

    /**
     * 获取参数列表
     */
    public List<Object> getParams() {
        return new ArrayList<>(params);
    }

    /**
     * 获取参数数量
     */
    public int getParamCount() {
        return params.size();
    }

    /**
     * 清空条件（保留 SELECT 和 FROM）
     */
    public NativeQueryBuilder<T> clearConditions() {
        String sqlStr = sql.toString();
        int whereIndex = sqlStr.toUpperCase().indexOf(" WHERE ");
        if (whereIndex != -1) {
            sql.setLength(whereIndex);
        }
        params.clear();
        hasWhere = false;
        return this;
    }

    /**
     * LIKE 模式枚举
     */
    public enum LikeMode {
        /** 全匹配: %value% */
        ANY("%%%s%%"),
        /** 前缀匹配: value% */
        START("%s%%"),
        /** 后缀匹配: %value */
        END("%%%s");

        private final String pattern;

        LikeMode(String pattern) {
            this.pattern = pattern;
        }

        public String format(String value) {
            return String.format(pattern, value);
        }
    }

    /**
     * 执行查询并返回 Map 列表。
     *
     * <p>统一使用 JPA 标准 {@link Tuple} 接口，避免依赖 Hibernate 私有 API
     * （{@code org.hibernate.transform.Transformers.ALIAS_TO_ENTITY_MAP} 在 Hibernate 6 已弃用）。
     * 若 SELECT 里用了 {@code AS alias} 显式命名，列名取 alias；否则退化为 {@code col_0 / col_1 ...}。</p>
     */
    public List<Map<String, Object>> listForMap() {
        return toMapList(build(Tuple.class).getResultList());
    }

    /**
     * 与 {@link #listForMap()} 语义完全一致，仅保留别名以便业务侧显式区分"位置列名"和"SQL 别名"。
     * 内部实现相同，均为 JPA {@link Tuple} 转换。
     */
    public List<Map<String, Object>> listForMapWithAlias() {
        return toMapList(build(Tuple.class).getResultList());
    }

    /**
     * 把 {@link Tuple} 列表转换为 {@code Map} 列表。
     * 有 alias 用 alias，无 alias 用 {@code col_N}。
     */
    private static List<Map<String, Object>> toMapList(List<Tuple> tuples) {
        List<Map<String, Object>> result = new ArrayList<>(tuples.size());
        for (Tuple tuple : tuples) {
            Map<String, Object> map = new LinkedHashMap<>();
            int positional = 0;
            for (TupleElement<?> element : tuple.getElements()) {
                String alias = element.getAlias();
                String key = (alias != null && !alias.isEmpty()) ? alias : "col_" + positional;
                map.put(key, tuple.get(element));
                positional++;
            }
            result.add(map);
        }
        return result;
    }
}