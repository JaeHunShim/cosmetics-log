package com.dream.cosmeticshop.adapter.in.kafka;

import com.dream.cosmeticshop.adapter.in.kafka.model.DebeziumMessage;
import com.dream.cosmeticshop.application.service.OrderEventProcessingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderEventListener {

    private static final String ORDER_TOPIC = "${order-topic-name}";

    private final ObjectMapper objectMapper;
    private final OrderEventProcessingService eventProcessingService;

    @KafkaListener(topics = ORDER_TOPIC)
    public void handleOrderEvent(String message) {
        log.debug("Received raw order event: {}", message);
        try {
            TypeReference<DebeziumMessage<DebeziumMessage.OrderPayload>> typeRef = new TypeReference<>() {};
            DebeziumMessage<DebeziumMessage.OrderPayload> event = objectMapper.readValue(message, typeRef);

            // INSERT('c') 또는 UPDATE('u') 이벤트일 경우, 'after' 데이터를 사용합니다.
            Optional.ofNullable(event.after()).ifPresent(this::routeEventByStatus);

        } catch (Exception e) {
            log.error("Error processing order event. message={}", message, e);
        }
    }

    private void routeEventByStatus(DebeziumMessage.OrderPayload orderPayload) {
        var orderDomain = orderPayload.toDomain();
        switch (orderDomain.status()) {
            case PAID -> eventProcessingService.processPaidOrder(orderDomain);
            case SHIPPED -> eventProcessingService.processShippedOrder(orderDomain);
            default -> log.info("Order status '{}' is not a target for processing.", orderDomain.status());
        }
    }
}
