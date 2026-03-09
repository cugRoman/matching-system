package com.stocktrade.matchingsystem.common.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RecentExecutionDTO {
    private String market;
    private String securityId;
    private String execId;
    private Integer execQty;
    private Double execPrice;
    private Instant executedAt;
    private Long executionCount;
}
