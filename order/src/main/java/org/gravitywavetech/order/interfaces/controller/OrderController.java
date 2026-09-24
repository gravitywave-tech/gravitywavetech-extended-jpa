package org.gravitywavetech.order.interfaces.controller;
import org.gravitywavetech.application.command.PayCommand;
import org.gravitywavetech.application.service.PaymentApplicationService;
import org.gravitywavetech.domain.model.Money;
import org.gravitywavetech.domain.model.OrderId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
/**
 * OrderController
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
@RestController
@RequestMapping("/order")
public class OrderController {
    private final PaymentApplicationService paymentApplicationService;

    public OrderController(PaymentApplicationService paymentApplicationService) {
        this.paymentApplicationService = paymentApplicationService;
    }

    @PostMapping("/pay")
    public String pay(@RequestParam Long orderId, @RequestParam BigDecimal payAmount){
        PayCommand cmd = new PayCommand(new OrderId(orderId), Money.of(payAmount));
        paymentApplicationService.handlePayCommand(cmd);
        return "success";
    }
}