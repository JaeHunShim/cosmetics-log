package com.dream.cosmeticshop.application.service;

import com.dream.cosmeticshop.application.port.out.SaveOrderSummaryPort;
import com.dream.cosmeticshop.application.port.out.SaveShipmentTrackingPort;
import com.dream.cosmeticshop.domain.Order;
import com.dream.cosmeticshop.domain.ShipmentTracking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProcessingService {
    private final SaveOrderSummaryPort saveOrderSummaryPort;
    private final SaveShipmentTrackingPort saveShipmentTrackingPort;

    @Transactional
    public void processPaidOrder(Order order) {
        saveOrderSummaryPort.save(order);
        log.info("Paid order summary has been saved for order number: {}", order.orderNumber());
    }

    @Transactional
    public void processShippedOrder(Order order) {
        ShipmentTracking tracking = ShipmentTracking.start(order);
        saveShipmentTrackingPort.save(tracking);
        log.info("Shipment tracking started for order number: {}", order.orderNumber());
        sendShippingNotification(order.userId(), order.orderNumber());
    }

    private void sendShippingNotification(Long userId, String orderNumber) {
        log.info("Sending shipping notification to user {} for order {}", userId, orderNumber);
    }
}
