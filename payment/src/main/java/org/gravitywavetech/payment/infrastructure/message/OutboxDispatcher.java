package org.gravitywavetech.payment.infrastructure.message;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.payment.domain.message.OutboxMessage;
import org.gravitywavetech.payment.domain.message.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Outbox 消息投递调度器。
 *
 * <p>定时扫描 t_outbox_message 中 status=PENDING 且 next_retry_time &lt;= now 的消息，
 * 逐条投递到消息中间件。采用乐观锁 + 单实例运行标志避免多实例/多轮次并发重复投递。
 *
 * <p>核心不变量：
 * <ul>
 *   <li>投递成功：markSent 后落库，不再重试</li>
 *   <li>投递失败：markFailed 递增 retryCount，按指数退避更新 nextRetryTime</li>
 *   <li>超过 MAX_RETRY：进入 FAILED 终态，等人工介入</li>
 *   <li>乐观锁冲突：另一实例已处理，跳过即可</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatcher {

    private final OutboxRepository outboxRepository;
    private final MessageSendPort sendPort;

    /** 单进程内防止并发轮次互相打架；多实例部署建议改用数据库分布式锁。 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${outbox.dispatcher.enabled:true}")
    private boolean enabled;

    @Value("${outbox.dispatcher.batch-size:50}")
    private int batchSize;

    @PostConstruct
    public void init() {
        log.info("Outbox 调度器初始化，enabled={}, batchSize={}", enabled, batchSize);
    }

    /**
     * 定时扫描并投递。fixedDelayString 允许从配置覆盖；默认 3 秒一次。
     * 单次投递失败不影响其他消息，也不会抛出到调度器。
     */
    @Scheduled(fixedDelayString = "${outbox.dispatcher.interval-ms:3000}")
    public void dispatch() {
        if (!enabled) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            log.debug("上一轮 Outbox 调度尚未完成，跳过本轮");
            return;
        }
        try {
            List<OutboxMessage> due = outboxRepository.findDueToSend(Instant.now(), batchSize);
            if (due.isEmpty()) {
                return;
            }
            log.info("Outbox 本轮扫描到 {} 条待投递消息", due.size());
            for (OutboxMessage msg : due) {
                dispatchSingle(msg);
            }
        } catch (Exception e) {
            log.error("Outbox 调度异常", e);
        } finally {
            running.set(false);
        }
    }

    private void dispatchSingle(OutboxMessage msg) {
        Instant now = Instant.now();
        try {
            sendPort.send(msg);
            msg.markSent(now);
            outboxRepository.update(msg);
            log.info("Outbox 消息投递成功，id={}, messageKey={}, retryCount={}",
                    msg.getId().getId(), msg.getMessageKey(), msg.getRetryCount());
        } catch (OptimisticLockingFailureException e) {
            log.warn("Outbox 消息已被其他实例处理，跳过，id={}", msg.getId().getId());
        } catch (Exception e) {
            msg.markFailed(now, e.getMessage());
            try {
                outboxRepository.update(msg);
            } catch (Exception updateErr) {
                log.error("Outbox 消息标记失败后状态更新异常，id={}", msg.getId().getId(), updateErr);
                return;
            }
            log.warn("Outbox 消息投递失败，将重试，id={}, messageKey={}, retryCount={}, status={}, error={}",
                    msg.getId().getId(), msg.getMessageKey(), msg.getRetryCount(), msg.getStatus(), e.getMessage());
        }
    }
}
