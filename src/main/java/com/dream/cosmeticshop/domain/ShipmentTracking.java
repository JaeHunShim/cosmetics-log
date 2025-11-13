package com.dream.cosmeticshop.domain;

import java.time.Instant;

public record ShipmentTracking(String orderNumber,
                               Long userId,
                               String shippingAddress,
                               ShipmentStatus status,
                               Instant trackingAt
) {
    public enum ShipmentStatus {
        PREPARING, SHIPPED, IN_TRANSIT, DELIVERED
    }

    public static ShipmentTracking start(Order order) {
        return new ShipmentTracking(
                order.orderNumber(),
                order.userId(),
                order.shippingAddress(),
                ShipmentStatus.SHIPPED,
                Instant.now()
        );
    }
}
