package com.stocktrade.matchingsystem.common.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserReportSummaryDTO {
    private String reportId;
    private String type;
    private String refClOrdId;
    private Instant ts;
    private Integer qty;
    private Double price;
    private Integer rejectCode;
    private String rejectText;
    private String execId;
    private Integer execQty;
    private Double execPrice;
    private String execSource;
    private String cancelText;
}
