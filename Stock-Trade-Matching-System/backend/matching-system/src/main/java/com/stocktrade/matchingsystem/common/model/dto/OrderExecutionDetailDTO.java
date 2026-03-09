package com.stocktrade.matchingsystem.common.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderExecutionDetailDTO {
    private String clOrderId;
    private String market;
    private String securityId;
    private String side;
    private Integer qty;
    private Integer qtyFilled;
    private Integer qtyRemaining;
    private String status;
    private Integer executionCount;
    private Integer totalExecutedQty;
    private Double avgExecutedPrice;
    private Instant lastExecutedAt;
    private List<ExecutionDetailItemDTO> executions;
}
