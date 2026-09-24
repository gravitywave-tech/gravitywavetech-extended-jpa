package org.gravitywavetech.order.domain.exception;

/**
 * OrderNotFoundException
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */
public class OrderNotFoundException  extends RuntimeException{
    public OrderNotFoundException(){
        super("订单不存在");
    }
}
