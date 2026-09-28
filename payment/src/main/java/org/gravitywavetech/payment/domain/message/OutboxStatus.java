package org.gravitywavetech.payment.domain.message;

/**
 * Outbox 消息投递状态。
 *
 * <p>Outbox 模式的核心不变量：只有 PENDING 状态的消息会被 Dispatcher 扫描投递，
 * 投递成功后置为 SENT（终态）；超过重试上限后转为 FAILED（终态，人工介入）。</p>
 */
public enum OutboxStatus {
    /** 待投递，业务事务内落库初始状态。 */
    PENDING,
    /** 已成功投递到消息中间件，终态。 */
    SENT,
    /** 超过最大重试次数仍未投递成功，终态，需人工/告警介入。 */
    FAILED
}
