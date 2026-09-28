package org.gravitywavetech.payment.domain.message;

import java.util.Objects;

/**
 * Outbox 消息 ID（雪花 ID）。
 *
 * <p>独立于业务主键，用于 Outbox 表主键；同时作为消息去重键的一部分。</p>
 */
public final class OutboxMessageId {

    private final Long id;

    public OutboxMessageId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("OutboxMessageId 必须为正数");
        }
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutboxMessageId other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
