package com.stocktrade.matchingsystem.common.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ExecutionDetailItemDTO {
    private String execId;
    private Integer execQty;
    private Double execPrice;
    private String execType;
    private String counterOrderId;
    private Instant executedAt;
}
