package org.gravitywavetech.inventory.infrastructure.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * P6Spy 自定义日志格式化器，直接输出完整可执行的 SQL 语句。
 */
public class CustomP6SpyLogger implements MessageFormattingStrategy {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }
        return String.format("[%s] 耗时 %d ms | SQL: %s",
                dateFormat.format(new Date()), elapsed, sql);
    }
}
