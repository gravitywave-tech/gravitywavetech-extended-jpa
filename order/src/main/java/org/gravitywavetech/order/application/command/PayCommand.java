package org.gravitywavetech.order.application.command;

import org.gravitywavetech.order.domain.model.Money;
import org.gravitywavetech.order.domain.model.OrderId;

/**
 * PayCommand
 *
 * <p></p>
 *
 * @author Administrator
 * @version 1.0
 * @since 2026/9/24
 */

public record PayCommand(OrderId orderId, Money amount) {}