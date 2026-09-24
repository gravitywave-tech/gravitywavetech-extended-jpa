package org.gravitywavetech.payment.infrastructure.adapter;

/**
 * AlipayGatewayAdapter
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlipayGatewayAdapter implements PaymentGateway {
    // 模拟支付宝SDK，实际引入sdk
    @Override
    public PaymentResult requestPayment(PaymentRequest req) {
        // ACL翻译：内部领域对象 → 外部第三方参数
        log.info("调用支付宝，订单号：{}，金额：{}", req.orderRef(), req.amount().getAmount());
        // 模拟返回
        return new PaymentResult("ALI202609240001", true);
    }
}

