package org.gravitywavetech.extended.jpa.util;

/**
 * 雪花算法（Snowflake）主键生成器。
 *
 * <p>64 位 {@code long} 布局：</p>
 * <ul>
 *     <li>bit 63     : 符号位，固定 0（可作有符号 {@code Long} 使用）</li>
 *     <li>bit 22-62  : 41 位毫秒时间戳，自 {@link #EPOCH} 起，约可用 69 年</li>
 *     <li>bit 12-21  : 10 位 worker id，最多 1024 个节点</li>
 *     <li>bit 0-11   : 12 位毫秒内序列号，单毫秒内可生成 4096 个</li>
 * </ul>
 *
 * <p>线程安全：由 {@code synchronized} 保证，同一毫秒内 sequence 单调递增；
 * 若单毫秒内 sequence 用尽，自旋等待下一毫秒；时钟回拨直接抛异常而非静默等待，
 * 避免生成重复 ID。</p>
 *
 * <p>Worker id 通过系统属性 {@code extended.jpa.snowflake.worker-id} 或环境变量
 * {@code EXTENDED_JPA_SNOWFLAKE_WORKER_ID} 配置，默认 0；取值范围 {@code [0, 1023]}，
 * 非法值在类初始化时快速失败。</p>
 *
 * <p>业务侧无需直接调用本类——框架仓储的 {@code save()} 会按 {@code @Id} 字段类型
 * （{@code Long}/{@code long}）自动接入本生成器。</p>
 *
 * @author gravitywavetech
 */
public final class SnowflakeUtil {

    /** 起始纪元：2024-01-01T00:00:00Z，41 位毫秒约可用 69 年。 */
    public static final long EPOCH = 1704067200000L;

    private static final int WORKER_ID_BITS = 10;
    private static final int SEQUENCE_BITS  = 12;

    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;  // 1023
    private static final long MAX_SEQUENCE  = (1L << SEQUENCE_BITS)  - 1;  // 4095

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                       // 12
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;      // 22

    private static final long WORKER_ID = resolveWorkerId();

    private static long lastTimestamp = -1L;
    private static long sequence      =  0L;

    private SnowflakeUtil() {
    }

    /**
     * 生成下一个雪花 ID。线程安全。
     *
     * @return 64 位雪花 ID（正数）
     * @throws IllegalStateException 若系统时钟回拨
     */
    public static synchronized long nextId() {
        long now = System.currentTimeMillis();
        if (now < lastTimestamp) {
            throw new IllegalStateException(
                    "Clock moved backwards; refusing to generate id for "
                            + (lastTimestamp - now) + " ms");
        }
        if (now == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // 单毫秒内 sequence 用尽，自旋等待下一毫秒
                now = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = now;
        return ((now - EPOCH) << TIMESTAMP_SHIFT)
                | (WORKER_ID << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 从生成的雪花 ID 反解毫秒时间戳（仅用于调试/观测）。
     */
    public static long extractTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT) + EPOCH;
    }

    private static long waitNextMillis(long last) {
        long now = System.currentTimeMillis();
        while (now <= last) {
            now = System.currentTimeMillis();
        }
        return now;
    }

    private static long resolveWorkerId() {
        String value = System.getProperty("extended.jpa.snowflake.worker-id");
        if (value == null || value.isEmpty()) {
            value = System.getenv("EXTENDED_JPA_SNOWFLAKE_WORKER_ID");
        }
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        long id;
        try {
            id = Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "extended.jpa.snowflake.worker-id must be an integer, got: " + value, e);
        }
        if (id < 0 || id > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "extended.jpa.snowflake.worker-id must be in [0, " + MAX_WORKER_ID
                            + "], got: " + id);
        }
        return id;
    }
}