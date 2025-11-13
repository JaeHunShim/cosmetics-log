package com.dream.cosmeticshop.adapter.out.persistence;

import com.dream.cosmeticshop.adapter.out.persistence.document.OrderSummaryDocument;
import com.dream.cosmeticshop.adapter.out.persistence.document.ShipmentTrackingDocument;
import com.dream.cosmeticshop.application.port.out.SaveOrderSummaryPort;
import com.dream.cosmeticshop.application.port.out.SaveShipmentTrackingPort;
import com.dream.cosmeticshop.domain.Order;
import com.dream.cosmeticshop.domain.ShipmentTracking;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements SaveOrderSummaryPort, SaveShipmentTrackingPort {

    private final MongoTemplate mongoTemplate;

    @Override
    public void save(Order order) {
        mongoTemplate.save(OrderSummaryDocument.fromDomain(order));

    }

    @Override
    public void save(ShipmentTracking shipmentTracking) {
        mongoTemplate.save(ShipmentTrackingDocument.fromDomain(shipmentTracking));

    }
}
