package org.gravitywavetech.payment.domain.message;

import org.gravitywavetech.extended.jpa.util.SnowflakeUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Outbox 消息聚合根。
 *
 * <p>Outbox 模式（本地消息表）的核心：把「跨服务的消息」和「业务状态变更」
 * 放到同一个数据库事务内提交，避免 RPC/MQ 直发失败导致状态与消息不一致。
 *
 * <p>不变量：
 * <ul>
 *   <li>messageKey 唯一（用于业务侧幂等）</li>
 *   <li>PENDING 消息的 nextRetryTime 不得超过当前时间 + 10 分钟</li>
 *   <li>SENT / FAILED 是终态，不再改变</li>
 *   <li>重试间隔采用指数退避：base * 2^(retryCount-1)，上限 5 分钟</li>
 * </ul>
 * </p>
 */
public final class OutboxMessage {

    private static final int MAX_RETRY = 5;
    private static final Duration RETRY_BASE = Duration.ofSeconds(3);
    private static final Duration RETRY_MAX = Duration.ofMinutes(5);

    private OutboxMessageId id;
    private String messageKey;
    private String destination;
    private String routingKey;
    private String payload;

    private OutboxStatus status;
    private int retryCount;
    private Instant nextRetryTime;
    private Instant sentAt;
    private String lastError;

    private OutboxMessage() {
    }

    /** 生成新的 OutboxMessageId（雪花 ID）。 */
    public static OutboxMessageId newId() {
        return new OutboxMessageId(SnowflakeUtil.nextId());
    }

    /**
     * 工厂方法：创建一条待投递的 Outbox 消息（初始 nextRetryTime = now）。
     */
    public static OutboxMessage create(OutboxMessageId id,
                                       String messageKey,
                                       String destination,
                                       String routingKey,
                                       String payload) {
        Objects.requireNonNull(messageKey, "messageKey");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(payload, "payload");
        OutboxMessage msg = new OutboxMessage();
        msg.id = id;
        msg.messageKey = messageKey;
        msg.destination = destination;
        msg.routingKey = routingKey;
        msg.payload = payload;
        msg.status = OutboxStatus.PENDING;
        msg.retryCount = 0;
        msg.nextRetryTime = Instant.now();
        return msg;
    }

    /**
     * 工厂方法：从持久化状态重建（用于读取后交给 Dispatcher 继续处理）。
     */
    public static OutboxMessage rehydrate(OutboxMessageId id,
                                          String messageKey,
                                          String destination,
                                          String routingKey,
                                          String payload,
                                          OutboxStatus status,
                                          int retryCount,
                                          Instant nextRetryTime,
                                          Instant sentAt,
                                          String lastError) {
        OutboxMessage msg = new OutboxMessage();
        msg.id = id;
        msg.messageKey = messageKey;
        msg.destination = destination;
        msg.routingKey = routingKey;
        msg.payload = payload;
        msg.status = status;
        msg.retryCount = retryCount;
        msg.nextRetryTime = nextRetryTime;
        msg.sentAt = sentAt;
        msg.lastError = lastError;
        return msg;
    }

    /** 投递成功：置为 SENT 终态，记录 sentAt。 */
    public void markSent(Instant now) {
        if (this.status == OutboxStatus.SENT) {
            return;
        }
        this.status = OutboxStatus.SENT;
        this.sentAt = now;
    }

    /**
     * 投递失败：记录本次错误 + 递增 retryCount。
     * 若已达到最大重试次数，转为 FAILED 终态；否则更新 nextRetryTime 为下一次退避时间。
     */
    public void markFailed(Instant now, String error) {
        if (this.status == OutboxStatus.SENT) {
            return;
        }
        this.lastError = error;
        this.retryCount = this.retryCount + 1;
        if (this.retryCount >= MAX_RETRY) {
            this.status = OutboxStatus.FAILED;
            return;
        }
        Duration delay = retryDelayFor(this.retryCount);
        this.nextRetryTime = now.plus(delay);
    }

    /** 是否到达可投递时间。 */
    public boolean isDueToSend(Instant now) {
        return this.status == OutboxStatus.PENDING
                && this.nextRetryTime != null
                && !this.nextRetryTime.isAfter(now);
    }

    private static Duration retryDelayFor(int retryCount) {
        long seconds = (long) (RETRY_BASE.getSeconds() * Math.pow(2, Math.max(0, retryCount - 1)));
        Duration d = Duration.ofSeconds(seconds);
        return d.compareTo(RETRY_MAX) > 0 ? RETRY_MAX : d;
    }

    // ========== getters ==========

    public OutboxMessageId getId() { return id; }
    public String getMessageKey() { return messageKey; }
    public String getDestination() { return destination; }
    public String getRoutingKey() { return routingKey; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public Instant getNextRetryTime() { return nextRetryTime; }
    public Instant getSentAt() { return sentAt; }
    public String getLastError() { return lastError; }
}
