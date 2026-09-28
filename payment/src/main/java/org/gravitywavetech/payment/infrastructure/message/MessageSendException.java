package org.gravitywavetech.payment.infrastructure.message;

/**
 * 消息发送失败异常。
 *
 * <p>不可恢复的编程错误（如参数缺失）也应通过该异常上抛，
 * Dispatcher 会将其转换为 Outbox 的 retry/failed 状态。</p>
 */
public class MessageSendException extends RuntimeException {

    public MessageSendException(String message) {
        super(message);
    }

    public MessageSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
