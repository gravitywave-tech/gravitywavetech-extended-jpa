package org.gravitywavetech.order.interfaces.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.order.application.command.CreateOrderCommand;
import org.gravitywavetech.order.application.command.PayCommand;
import org.gravitywavetech.order.application.command.UpdateOrderCommand;
import org.gravitywavetech.order.application.dto.OrderResponse;
import org.gravitywavetech.order.application.service.OrderApplicationService;
import org.gravitywavetech.order.application.service.PaymentApplicationService;
import org.gravitywavetech.order.domain.model.Money;
import org.gravitywavetech.order.domain.model.OrderId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口。
 *
 * <p>暴露订单聚合的完整 CRUD + 支付入口。业务规则由 Order 聚合根内部保证，
 * Controller 只做协议转换（HTTP ↔ Command/DTO）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderApplicationService orderApplicationService;
    private final PaymentApplicationService paymentApplicationService;

    /** 创建订单。 */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderCommand cmd) {
        OrderResponse response = orderApplicationService.createOrder(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** 查询单个订单。 */
    @GetMapping("/{orderId}")
    public OrderResponse get(@PathVariable Long orderId) {
        return orderApplicationService.getOrder(orderId);
    }

    /** 列出全部订单（demo 用，生产请加分页）。 */
    @GetMapping
    public List<OrderResponse> list() {
        return orderApplicationService.listOrders();
    }

    /** 修改订单（当前仅支持收货地址）。 */
    @PutMapping("/{orderId}")
    public OrderResponse update(@PathVariable Long orderId, @RequestBody UpdateOrderCommand cmd) {
        return orderApplicationService.updateOrder(orderId, cmd);
    }

    /** 取消订单（软删除，已支付订单会触发退款事件）。 */
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable Long orderId) {
        return orderApplicationService.cancelOrder(orderId);
    }

    /** 物理删除订单（仅 WAITING_PAYMENT 状态允许）。 */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete(@PathVariable Long orderId) {
        orderApplicationService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 发起支付。
     *
     * <p>自动使用订单 totalAmount 作为支付金额，避免用户输入错金额导致状态不一致。</p>
     */
    @PostMapping("/{orderId}/pay")
    public String pay(@PathVariable Long orderId) {
        OrderResponse order = orderApplicationService.getOrder(orderId);
        PayCommand cmd = new PayCommand(
                new OrderId(orderId),
                Money.of(order.totalAmount())
        );
        paymentApplicationService.handlePayCommand(cmd);
        return "payment initiated";
    }
}
