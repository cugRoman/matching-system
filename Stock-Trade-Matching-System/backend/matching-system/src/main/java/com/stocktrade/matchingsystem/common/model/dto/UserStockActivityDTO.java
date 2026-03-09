package com.stocktrade.matchingsystem.common.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserStockActivityDTO {
    private String market;
    private String securityId;
    private Integer totalOrderQty;
    private Integer totalExecQty;
    private List<UserOrderSummaryDTO> orders;
    private List<UserReportSummaryDTO> reports;
}
