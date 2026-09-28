package org.gravitywavetech.payment.infrastructure.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Outbox 消息体 JSON 序列化。
 *
 * <p>Spring Boot Web 已自动装配 ObjectMapper（含 JavaTimeModule）；本类只做薄封装，
 * 把 JSON 序列化失败统一转换为 {@link MessageSendException} 供 Dispatcher 处理。</p>
 */
@Component
@RequiredArgsConstructor
public class OutboxJsonSupport {

    private final ObjectMapper objectMapper;

    public String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new MessageSendException("Outbox 消息序列化失败，type="
                    + (payload == null ? "null" : payload.getClass().getName()), e);
        }
    }
}
