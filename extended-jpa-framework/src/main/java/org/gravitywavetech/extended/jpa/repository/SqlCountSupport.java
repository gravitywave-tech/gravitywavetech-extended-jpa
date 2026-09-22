package org.gravitywavetech.extended.jpa.repository;

/**
 * COUNT 语句生成的唯一实现。
 *
 * <p>此前 Native SQL 与 HQL 的 COUNT 生成存在多份实现（基类、{@code NativeQueryBuilder}、
 * {@code NativeQueryFragmentImpl}、{@code HqlQueryFragmentImpl}），行为并不一致：
 * 同一个分页语义在不同入口下会生成不同的 COUNT 语句。这里收敛为唯一入口，
 * 使 {@code NativeQueryBuilder} / {@code HqlQueryBuilder} 与各查询片段共用同一套规则。</p>
 *
 * <p>HQL 的关键字检测使用字符扫描 + 词边界匹配（避免把 {@code SELECTED_FLAG} /
 * {@code myFromCol} 误判为关键字），并区分顶层与子查询上下文，避免嵌套子查询的
 * {@code SELECT} 被误判为主查询结构。</p>
 *
 * @author Administrator
 * @version 1.0
 */
public final class SqlCountSupport {

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

        // 顶层 SELECT 起始位置：可能是 "SELECT ... FROM ..."，也可能是 HQL 允许的省略形式 "FROM ..."
        int selectIdx = findTopLevelKeyword(upper, "SELECT", 0);
        int fromIdx = findTopLevelKeyword(upper, "FROM", selectIdx >= 0 ? selectIdx + 6 : 0);

        // 顶层非 SELECT 前缀的 FROM，或 FROM 之前又出现 SELECT（子查询），均视为复杂 HQL
        if (fromIdx < 0 || selectIdx < 0) {
            throw new IllegalStateException(
                    "无法自动生成 COUNT 语句（缺少合法的 SELECT ... FROM 结构），" +
                            "请通过 countHql(\"SELECT COUNT(*) ...\") 显式设置。原始 HQL: " + hql);
        }

        // 顶层 GROUP BY / UNION 无法安全改写
        if (findTopLevelKeyword(upper, "GROUP BY", fromIdx) >= 0
                || findTopLevelKeyword(upper, "UNION", fromIdx) >= 0) {
            throw new IllegalStateException(
                    "无法为包含 GROUP BY / UNION 的 HQL 自动生成 COUNT 语句，" +
                            "请通过 countHql(\"SELECT COUNT(*) ...\") 显式设置");
        }

        // 从 FROM 开始找顶层 ORDER BY，剥离之（COUNT 不需要排序）
        int orderByIdx = findTopLevelKeyword(upper, "ORDER BY", fromIdx);
        String baseHql = trimmed;
        if (orderByIdx > fromIdx) {
            baseHql = trimmed.substring(0, orderByIdx);
        }

        return "SELECT COUNT(*) " + baseHql.substring(fromIdx).trim();
    }

    /**
     * 从 {@code from} 位置起扫描，返回第一个位于括号深度 0、前后为词边界的 {@code keyword} 起始下标；
     * 未找到返回 -1。词边界定义：前后均为非字母/数字/下划线，或位于字符串首尾。
     */
    private static int findTopLevelKeyword(String s, String keyword, int from) {
        int depth = 0;
        int n = s.length();
        int i = from;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
                i++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    depth = 0;
                }
                i++;
            } else if (depth == 0 && i + keyword.length() <= n && s.startsWith(keyword, i)) {
                if (isWordBoundary(s, i - 1) && isWordBoundary(s, i + keyword.length())) {
                    return i;
                }
                i++;
            } else {
                i++;
            }
        }
        return -1;
    }

    private static boolean isWordBoundary(String s, int idx) {
        if (idx < 0 || idx >= s.length()) {
            return true;
        }
        char c = s.charAt(idx);
        return !Character.isLetterOrDigit(c) && c != '_';
    }
}
