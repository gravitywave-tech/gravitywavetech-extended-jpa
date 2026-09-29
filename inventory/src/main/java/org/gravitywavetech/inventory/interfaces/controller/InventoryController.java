package org.gravitywavetech.inventory.interfaces.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.inventory.application.command.CreateStockCommand;
import org.gravitywavetech.inventory.application.command.ReplenishStockCommand;
import org.gravitywavetech.inventory.application.dto.StockResponse;
import org.gravitywavetech.inventory.application.service.InventoryApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存接口。
 *
 * <p>暴露库存聚合的完整 CRUD + 扣减入口。业务规则由 Inventory 聚合根内部保证，
 * Controller 只做协议转换（HTTP ↔ Command/DTO）。</p>
 *
 * <p>典型调用方：
 * <ul>
 *   <li>管理员 / 运营：创建库存、补货、查询</li>
 *   <li>order 服务：通过 Spring Cloud Stream 异步扣减（不走本 Controller）</li>
 *   <li>其他服务：同步扣减（如秒杀场景预占）</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryApplicationService inventoryApplicationService;

    /** 初始化库存记录（管理员用，幂等由服务层保证）。 */
    @PostMapping("/stock")
    public ResponseEntity<StockResponse> create(@RequestBody CreateStockCommand cmd) {
        StockResponse response = inventoryApplicationService.createStock(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** 补货。 */
    @PostMapping("/stock/{productId}/replenish")
    public StockResponse replenish(
            @PathVariable Long productId,
            @RequestBody ReplenishStockCommand cmd) {
        ReplenishStockCommand replenishCmd = new ReplenishStockCommand(productId, cmd.quantity());
        return inventoryApplicationService.replenishStock(replenishCmd);
    }

    /** 手动扣减库存（测试或同步扣减场景）。 */
    @PostMapping("/stock/{productId}/deduct")
    public StockResponse deduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(required = false) Long orderId) {
        return inventoryApplicationService.deductStock(productId, quantity, orderId);
    }

    /** 查询单个商品库存。 */
    @GetMapping("/stock/{productId}")
    public StockResponse get(@PathVariable Long productId) {
        return inventoryApplicationService.getStock(productId);
    }

    /** 列出全部库存（demo 用，生产请加分页）。 */
    @GetMapping("/stock")
    public List<StockResponse> list() {
        return inventoryApplicationService.listStock();
    }
}
