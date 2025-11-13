package com.dream.cosmeticshop.adapter.out.persistence.document;

import com.dream.cosmeticshop.domain.Order;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "order_summaries")
public record OrderSummaryDocument(
        @Id String id,
        Long orderId,
        Long userId,
        String orderNumber,
        String status,
        BigDecimal totalAmount,
        Instant orderedAt,
        Instant processedAt
) {
    public static OrderSummaryDocument fromDomain(Order domain) {
        return new OrderSummaryDocument(
                null,
                domain.id(),
                domain.userId(),
                domain.orderNumber(),
                domain.status().name(),
                domain.totalAmount(),
                domain.orderedAt(),
                Instant.now()
        );
    }
}
