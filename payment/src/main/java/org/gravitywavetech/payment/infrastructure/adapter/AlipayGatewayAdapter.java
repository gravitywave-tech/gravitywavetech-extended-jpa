package org.gravitywavetech.payment.infrastructure.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付网关适配器。
 *
 * <p>当前为模拟实现，实际接入时替换为真实 SDK 调用即可。</p>
 */
@Slf4j
@Component
public class AlipayGatewayAdapter implements PaymentGateway {
    // 模拟支付宝SDK，实际引入sdk
    @Override
    public PaymentResult requestPayment(PaymentRequest req) {
        // ACL翻译：内部领域对象 → 外部第三方参数
        log.info("调用支付宝，订单号：{}，金额：{}", req.orderRef(), req.amount().getAmount());
        // 模拟返回
        return new PaymentResult("ALI" + System.currentTimeMillis(), true);
    }
}
