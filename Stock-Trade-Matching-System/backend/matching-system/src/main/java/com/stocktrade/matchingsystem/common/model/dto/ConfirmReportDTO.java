package com.stocktrade.matchingsystem.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 确认回报 DTO — 交易所确认已接收并受理了订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmReportDTO {

    private String clOrderId;
    private String market;
    private String securityId;
    private String side;
    private Integer qty;
    private Double price;
    private String shareHolderId;
}
