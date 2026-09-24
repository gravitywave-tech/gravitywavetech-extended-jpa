package org.gravitywavetech.order.domain.model;
import lombok.EqualsAndHashCode;

/**
 * OrderId
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */

@EqualsAndHashCode
public class OrderId {
    private final Long id;

    public OrderId(Long id){
        if(id == null || id <=0){
            throw new IllegalArgumentException("订单id非法");
        }
        this.id = id;
    }
    public Long getId(){
        return id;
    }
}
