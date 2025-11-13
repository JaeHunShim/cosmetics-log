package com.dream.cosmeticshop.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Order(Long id,
                    Long userId,
                    String orderNumber,
                    OrderStatus status,
                    BigDecimal totalAmount,
                    String paymentMethod,
                    String shippingAddress,
                    String shippingMemo,
                    Instant orderedAt
) {
    public enum OrderStatus {
        CREATED, PAID, SHIPPED, CANCELLED, REFUNDED, FAILED
    }

}
