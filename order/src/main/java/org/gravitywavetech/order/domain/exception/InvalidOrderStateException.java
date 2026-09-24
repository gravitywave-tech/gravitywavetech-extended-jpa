package org.gravitywavetech.order.domain.exception;

/**
 * InvalidOrderStateException
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */

public class InvalidOrderStateException extends RuntimeException{
    public InvalidOrderStateException(String msg){
        super(msg);
    }
}

