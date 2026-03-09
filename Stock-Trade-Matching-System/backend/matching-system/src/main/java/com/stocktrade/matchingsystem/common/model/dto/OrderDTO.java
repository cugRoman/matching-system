package com.stocktrade.matchingsystem.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 交易订单 DTO — 客户提交的买入/卖出请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    /** 订单唯一编号 */
    private String clOrderId;

    /** 市场代码: XSHG / XSHE / BJSE */
    private String market;

    /** 股票代码, 6位数字 */
    private String securityId;

    /** 买卖方向: B=买入, S=卖出 */
    private String side;

    /** 订单数量 (股) */
    private Integer qty;

    /** 订单价格 (元) */
    private Double price;

    /** 股东号 */
    private String shareHolderId;

    /** 交易日期 (可选，默认使用系统日期) */
    private LocalDate tradeDate;
}
