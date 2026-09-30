package org.gravitywavetech.order.application.listener;

import org.gravitywavetech.order.application.port.NotificationPort;
import org.gravitywavetech.order.application.port.StockDeductionPort;
import org.gravitywavetech.order.domain.model.OrderPaidEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * OrderPaidListener
 *
 * <p>订阅 {@link OrderPaidEvent}（Order 聚合在 pay 行为内产生），
 * 把领域事件翻译为跨上下文副作用：</p>
 * <ul>
 *   <li>扣减库存 —— 经 {@link StockDeductionPort} 通知 inventory 上下文；</li>
 *   <li>通知用户 —— 经 {@link NotificationPort}。</li>
 * </ul>
 * 两个副作用都只依赖出站端口，不感知具体实现（MQ / 短信网关）。
 */
@Component
public class OrderPaidListener {
    private final StockDeductionPort stockDeductionPort;
    private final NotificationPort notificationPort;

    public OrderPaidListener(StockDeductionPort stockDeductionPort, NotificationPort notificationPort) {
        this.stockDeductionPort = stockDeductionPort;
        this.notificationPort = notificationPort;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        // 事务提交之后异步执行，避免主事务阻塞
        stockDeductionPort.deductForOrder(event.getOrderId(), event.getItems());
        notificationPort.notifyOrderPaid(event.getOrderId());
    }
}