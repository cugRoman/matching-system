package com.stocktrade.matchingsystem.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 撤单请求 DTO — 客户请求撤回已提交的订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelRequestDTO {

    /** 撤单请求的唯一编号 */
    private String orderId;

    /** 待撤原始订单的编号 */
    private String origClientId;

    /** 待撤订单的市场 */
    private String market;

    /** 待撤订单的股票代码 */
    private String securityId;

    /** 待撤订单的股东号 */
    private String shareHolderId;

    /** 待撤订单的买卖方向 */
    private String side;
}
