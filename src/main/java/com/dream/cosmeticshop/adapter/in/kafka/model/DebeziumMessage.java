package com.dream.cosmeticshop.adapter.in.kafka.model;

import com.dream.cosmeticshop.domain.Order;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DebeziumMessage<T>(
        T before,
        T after,
        Source source,
        String op,
        @JsonProperty("ts_ms") long tsMs
) {

    // Debezium 메시지 안의 'source' 필드에 해당하는 클래스
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(
            String version,
            String connector,
            String name,
            @JsonProperty("ts_ms") long tsMs,
            String db,
            String table
    ) {
    }

    // 우리의 'orders' 테이블 데이터에 해당하는 클래스
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderPayload(
            long id,
            @JsonProperty("user_id") long userId,
            @JsonProperty("order_number") String orderNumber,
            String status,
            // MySQL의 decimal/numeric 타입은 Debezium에서 문자열로 올 수 있으므로, String으로 받습니다.
            @JsonProperty("total_amount") BigDecimal totalAmount,
            @JsonProperty("payment_method") String paymentMethod,
            @JsonProperty("shipping_address") String shippingAddress,
            @JsonProperty("shipping_memo") String shippingMemo,
            // Debezium의 타임스탬프는 보통 long 타입의 epoch milliseconds 입니다.
            @JsonProperty("ordered_at") long orderedAt,
            @JsonProperty("updated_at") long updatedAt
    ) {
        public Order toDomain() {
            return new Order(
                    this.id,
                    this.userId,
                    this.orderNumber,
                    Order.OrderStatus.valueOf(this.status),
                    this.totalAmount,
                    this.paymentMethod,
                    this.shippingAddress,
                    this.shippingMemo,
                    Instant.ofEpochMilli(this.orderedAt)
            );
        }
    }
}