package com.stocktrade.matchingsystem.common.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserOrderSummaryDTO {
    private String clOrderId;
    private String market;
    private String securityId;
    private String side;
    private Integer qty;
    private Double price;
    private String status;
    private Integer qtyFilled;
    private Integer qtyRemaining;
    private Instant recvTime;
    private Instant updatedAt;
}
