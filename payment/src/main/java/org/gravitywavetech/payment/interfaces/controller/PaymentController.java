package org.gravitywavetech.payment.interfaces.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gravitywavetech.payment.application.service.PaymentApplicationService;
import org.gravitywavetech.payment.domain.model.PaymentId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付网关回调接口。
 *
 * <p>用于接收第三方支付平台（支付宝/微信等）的异步通知，
 * 触发 Payment 聚合的 markSuccess / markFailed 领域行为。</p>
 */
@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;

    @PostMapping("/callback")
    public String handleCallback(
            @RequestParam Long paymentId,
            @RequestParam(defaultValue = "true") boolean success,
            @RequestParam(defaultValue = "") String tradeNo,
            @RequestParam(defaultValue = "") String failMsg) {
        log.info("接收支付网关回调，paymentId={}, success={}, tradeNo={}",
                paymentId, success, tradeNo);
        paymentApplicationService.handlePaymentCallback(
                new PaymentId(paymentId), tradeNo, success, failMsg);
        return "success";
    }
}
