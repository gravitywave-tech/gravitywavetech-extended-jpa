package org.gravitywavetech.extended.jpa.repository;

import org.springframework.util.Assert;

/**
 * 分页相关的通用工具。
 *
 * <p>JPA {@code setFirstResult(int)} 只接受 {@code int}，而 {@code Pageable#getOffset()} 返回
 * {@code long}。直接强转在 offset 溢出时会静默转成负数，让 Hibernate 在方言层面报错，
 * 报错信息与根因相距甚远。这里把校验收敛到一个入口，避免每个 Builder / Fragment 各自重复。</p>
 *
 * @author gravitywavetech
 */
public final class PageSupport {

    private PageSupport() {
    }

    /**
     * 校验 {@code long} 类型的分页 offset 落在 {@code [0, Integer.MAX_VALUE]} 区间，并转换为 {@code int}。
     *
     * @param offset {@code Pageable#getOffset()} 返回值
     * @return 转换后的 int
     * @throws IllegalArgumentException offset 为负数或超出 int 上限
     */
    public static int offsetToInt(long offset) {
        Assert.isTrue(offset >= 0 && offset <= Integer.MAX_VALUE,
                "page offset must be in [0, Integer.MAX_VALUE], got: " + offset);
        return (int) offset;
    }
}