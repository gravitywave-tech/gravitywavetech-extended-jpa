package org.gravitywavetech.payment.domain.message;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbox 仓储端口（DDD）。
 *
 * <p>实现位于 infrastructure 层，采用乐观锁避免 Dispatcher 多实例并发修改同一条消息。</p>
 */
public interface OutboxRepository {

    /** 与业务状态变更在同一事务内落库。 */
    OutboxMessage save(OutboxMessage message);

    Optional<OutboxMessage> findById(OutboxMessageId id);

    /** 按 messageKey 幂等查询，避免同一业务事件重复写入。 */
    Optional<OutboxMessage> findByMessageKey(String messageKey);

    /**
     * 扫描当前可投递的消息（status = PENDING AND nextRetryTime <= now）。
     * 建议实现使用分页或 LIMIT 防止一次扫描过多。
     */
    List<OutboxMessage> findDueToSend(Instant now, int limit);

    /**
     * 更新状态（投递成功/失败），依赖 @Version 乐观锁失败会抛出异常。
     */
    OutboxMessage update(OutboxMessage message);
}
