package org.gravitywavetech.framework.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * CustomP6SpyLogger
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/8/13
 */
public class CustomP6SpyLogger implements MessageFormattingStrategy {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        // 如果sql为空，可能不记录
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }
        // 返回包含时间、耗时、SQL的格式化字符串
        return String.format("[%s] 耗时 %d ms | SQL: %s",
                dateFormat.format(new Date()), elapsed, sql);
    }
}
