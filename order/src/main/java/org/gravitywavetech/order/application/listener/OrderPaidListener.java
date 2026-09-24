package org.gravitywavetech.order.application.listener;

import org.gravitywavetech.order.domain.model.OrderPaidEvent;
import org.gravitywavetech.order.infrastructure.client.InventoryClient;
import org.gravitywavetech.order.infrastructure.client.NotificationClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * OrderPaidListener
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@Component
public class OrderPaidListener {
    private final InventoryClient inventoryClient;
    private final NotificationClient notificationClient;

    public OrderPaidListener(InventoryClient inventoryClient, NotificationClient notificationClient) {
        this.inventoryClient = inventoryClient;
        this.notificationClient = notificationClient;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        // 事务提交之后异步执行，避免主事务阻塞
        inventoryClient.deduct(event.getOrderId());
        notificationClient.notifyUser(event.getOrderId());
    }
}