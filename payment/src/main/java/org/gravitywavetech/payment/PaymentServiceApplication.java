package org.gravitywavetech.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment 服务主启动类。
 *
 * <p>{@link EnableScheduling} 启用 Outbox 调度器（{@code OutboxDispatcher}）的
 * {@code @Scheduled} 任务；{@link EnableAsync} 保留给未来异步执行场景。</p>
 */
@SpringBootApplication(scanBasePackages = "org.gravitywavetech")
@EnableAsync
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
