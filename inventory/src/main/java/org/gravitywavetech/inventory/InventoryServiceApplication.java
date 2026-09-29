package org.gravitywavetech.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 库存服务启动类。
 *
 * <p>独立进程，独立 H2/MySQL 数据源，独立 Eureka 注册，
 * 通过 Spring Cloud Stream 接收 order 服务发来的扣减库存消息。</p>
 */
@SpringBootApplication(scanBasePackages = "org.gravitywavetech")
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
