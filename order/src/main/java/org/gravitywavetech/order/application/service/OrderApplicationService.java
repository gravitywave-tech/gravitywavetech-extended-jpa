package org.gravitywavetech.order.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.extended.jpa.util.SnowflakeUtil;
import org.gravitywavetech.order.application.command.CreateOrderCommand;
import org.gravitywavetech.order.application.command.UpdateOrderCommand;
import org.gravitywavetech.order.application.dto.OrderResponse;
import org.gravitywavetech.order.domain.event.OrderCreatedEvent;
import org.gravitywavetech.order.domain.model.Address;
import org.gravitywavetech.order.domain.model.Order;
import org.gravitywavetech.order.domain.model.OrderId;
import org.gravitywavetech.order.domain.model.OrderItem;
import org.gravitywavetech.order.domain.model.Money;
import org.gravitywavetech.order.domain.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单应用服务。
 *
 * <p>负责编排「创建 / 修改 / 取消 / 查询」四类业务用例，
 * 保持 Order 聚合的领域规则在 Order 内部，本服务只做流程调度 + 事件发布。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建订单。
     *
     * <p>主键由 {@link SnowflakeUtil} 生成（业务侧唯一标识，用于跨服务消息传递），
     * 保存后发布 {@link OrderCreatedEvent} 触发下游（通知、日志审计等）。</p>
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderCommand cmd) {
        if (cmd.buyerId() == null) {
            throw new IllegalArgumentException("buyerId 不能为空");
        }
        if (cmd.items() == null || cmd.items().isEmpty()) {
            throw new IllegalArgumentException("订单项不能为空");
        }

        List<OrderItem> items = cmd.items().stream()
                .map(it -> new OrderItem(
                        it.productId(),
                        it.productName(),
                        it.quantity(),
                        Money.of(it.unitPrice())
                ))
                .toList();

        OrderId orderId = new OrderId(SnowflakeUtil.nextId());
        Address address = Address.of(cmd.addressProvince(), cmd.addressCity(), cmd.addressDetail());
        Order order = Order.create(orderId, cmd.buyerId(), items, address);

        orderRepository.save(order);

        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        order.clearDomainEvents();

        log.info("订单创建成功，orderId={}, totalAmount={}", orderId.getId(), order.getTotalAmount().getAmount());
        return OrderResponse.from(order);
    }

    /**
     * 修改订单（当前仅支持收货地址）。
     *
     * <p>调用 Order 聚合的 updateAddress 领域行为，由 Order 内部校验状态合法性。</p>
     */
    @Transactional
    public OrderResponse updateOrder(Long orderId, UpdateOrderCommand cmd) {
        Order order = orderRepository.findById(new OrderId(orderId));
        Address newAddress = Address.of(cmd.addressProvince(), cmd.addressCity(), cmd.addressDetail());
        order.updateAddress(newAddress);
        orderRepository.save(order);
        log.info("订单地址已更新，orderId={}", orderId);
        return OrderResponse.from(order);
    }

    /**
     * 取消订单。
     *
     * <p>业务层删除：调用 Order 聚合的 cancel 领域行为，已支付订单会触发退款事件。</p>
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(new OrderId(orderId));
        order.cancel();
        orderRepository.save(order);

        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        order.clearDomainEvents();

        log.info("订单已取消，orderId={}", orderId);
        return OrderResponse.from(order);
    }

    /**
     * 物理删除订单。
     *
     * <p>谨慎使用：仅当订单仍处于 WAITING_PAYMENT 状态时允许，避免误删已支付订单。</p>
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(new OrderId(orderId));
        if (order.getStatus() != org.gravitywavetech.order.domain.model.OrderStatus.WAITING_PAYMENT) {
            throw new org.gravitywavetech.order.domain.exception.InvalidOrderStateException(
                    "仅未支付订单允许物理删除，当前状态=" + order.getStatus());
        }
        orderRepository.delete(order);
        log.info("订单已物理删除，orderId={}", orderId);
    }

    /**
     * 查询单个订单。
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(new OrderId(orderId));
        return OrderResponse.from(order);
    }

    /**
     * 列出全部订单（无分页，仅 demo 用）。
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }
}
