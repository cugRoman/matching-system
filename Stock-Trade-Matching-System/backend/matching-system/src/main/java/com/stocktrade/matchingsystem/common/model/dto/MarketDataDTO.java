package com.stocktrade.matchingsystem.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 行情数据 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataDTO {

    /** 市场代码 */
    private String market;

    /** 股票代码 */
    private String securityId;

    /** 当前最优买价（买方愿出的最高价） */
    private Double bidPrice;

    /** 当前最优卖价（卖方愿接受的最低价） */
    private Double askPrice;
}
