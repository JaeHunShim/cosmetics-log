package com.dream.cosmeticshop.adapter.out.persistence.document;

import com.dream.cosmeticshop.domain.ShipmentTracking;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "shipment_tracking")
public record ShipmentTrackingDocument(
        @Id String id,
        String orderNumber,
        Long userId,
        String shippingAddress,
        String status,
        Instant trackingAt
) {
    public static ShipmentTrackingDocument fromDomain(ShipmentTracking domain) {
        return new ShipmentTrackingDocument(
                null,
                domain.orderNumber(),
                domain.userId(),
                domain.shippingAddress(),
                domain.status().name(),
                domain.trackingAt()
        );
    }
}
