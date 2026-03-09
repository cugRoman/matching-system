package com.stocktrade.matchingsystem.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成交回报 DTO — 交易所或内部撮合产生的成交结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecReportDTO {

    /** 原始订单编号 */
    private String clOrderId;

    /** 市场 */
    private String market;

    /** 股票代码 */
    private String securityId;

    /** 买卖方向 */
    private String side;

    /** 原始订单数量 */
    private Integer qty;

    /** 原始订单价格 */
    private Double price;

    /** 股东号 */
    private String shareHolderId;

    /** 成交编号 (唯一) */
    private String execId;

    /** 本次成交数量 */
    private Integer execQty;

    /** 本次成交价格 */
    private Double execPrice;

    /** exec source: INTERNAL | EXCHANGE */
    private String execSource;
}
