package com.stocktrade.matchingsystem.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单非法回报 DTO — 交易所拒绝 或 本系统校验/风控拦截
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectReportDTO {

    /** 原始订单编号 */
    private String clOrderId;

    /** 市场 */
    private String market;

    /** 股票代码 */
    private String securityId;

    /** 买卖方向 */
    private String side;

    /** 订单数量 */
    private Integer qty;

    /** 订单价格 */
    private Double price;

    /** 股东号 */
    private String shareHolderId;

    /** 拒绝码 */
    private Integer rejectCode;

    /** 拒绝原因说明 */
    private String rejectText;
}
