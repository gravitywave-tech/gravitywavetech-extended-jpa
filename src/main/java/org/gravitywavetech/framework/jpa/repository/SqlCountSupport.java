package org.gravitywavetech.framework.jpa.repository;

/**
 * COUNT 语句生成的唯一实现。
 *
 * <p>此前 Native SQL 与 HQL 的 COUNT 生成存在多份实现（基类、{@code NativeQueryBuilder}、
 * {@code NativeQueryFragmentImpl}、{@code HqlQueryFragmentImpl}），行为并不一致：
 * 同一个分页语义在不同入口下会生成不同的 COUNT 语句。这里收敛为唯一入口，
 * 使 {@code NativeQueryBuilder} / {@code HqlQueryBuilder} 与各查询片段共用同一套规则。</p>
 *
 * @author Administrator
 * @version 1.0
 */
public final class SqlCountSupport {

    private static final String FROM = " FROM ";
    private static final String ORDER_BY = " ORDER BY ";
    private static final String GROUP_BY = "GROUP BY";
    private static final String UNION = " UNION ";
    private static final String SELECT = "SELECT";
    private static final int SELECT_KEYWORD_LENGTH = 7;

    private SqlCountSupport() {
    }

    /**
     * 构建 Native SQL 的 COUNT 语句。
     *
     * <p>统一采用子查询包裹：{@code SELECT COUNT(*) FROM (原SQL) t}。
     * 不对原 SQL 做任何字符串解析，因此对 GROUP BY、DISTINCT、多表 JOIN、子查询等
     * 任意复杂 SQL 都成立（含 GROUP BY 时得到的是分组数，正是分页所需语义）。</p>
     *
     * @param sql 原始查询 SQL
     * @return COUNT SQL
     */
    public static String buildCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql.trim() + ") t";
    }

    /**
     * 构建 HQL 的 COUNT 语句。
     *
     * <p>HQL 不支持把子查询作为 FROM 子句，因此只能在字符串层面剥离 SELECT 与 ORDER BY。
     * 对无法安全剥离的场景（GROUP BY / UNION / 子查询）直接抛出异常，
     * 由调用方通过显式指定 count 语句来解决，避免生成语义错误的 COUNT 后
     * 在 {@code getSingleResult()} 处才暴露问题。</p>
     *
     * @param hql 原始查询 HQL
     * @return COUNT HQL
     * @throws IllegalStateException    含 GROUP BY / UNION / 子查询，无法自动生成
     * @throws IllegalArgumentException HQL 缺少 FROM 子句
     */
    public static String buildCountHql(String hql) {
        String trimmed = hql.trim();
        String upper = trimmed.toUpperCase();

        if (upper.contains(GROUP_BY) || upper.contains(UNION) || upper.indexOf(SELECT, SELECT_KEYWORD_LENGTH) > 0) {
            throw new IllegalStateException(
                    "无法为包含 GROUP BY / UNION / 子查询的 HQL 自动生成 COUNT 语句，" +
                            "请通过 countHql(\"SELECT COUNT(*) ...\") 显式设置");
        }

        // 兼容无 SELECT 前缀的 HQL，例如 "FROM PsLawcase t WHERE ..."
        int fromIndex = upper.indexOf(FROM);
        if (fromIndex == -1 && upper.startsWith("FROM ")) {
            fromIndex = 0;
        }
        if (fromIndex == -1) {
            throw new IllegalArgumentException("Invalid HQL, missing FROM clause: " + hql);
        }

        // 安全剥离 ORDER BY（从末尾查找，避免误伤字段中的关键字），并去除拼接产生的多余空格
        int orderByIndex = upper.lastIndexOf(ORDER_BY);
        String baseHql = trimmed;
        if (orderByIndex != -1 && orderByIndex > fromIndex) {
            baseHql = trimmed.substring(0, orderByIndex);
        }

        return "SELECT COUNT(*) " + baseHql.substring(fromIndex).trim();
    }
}
