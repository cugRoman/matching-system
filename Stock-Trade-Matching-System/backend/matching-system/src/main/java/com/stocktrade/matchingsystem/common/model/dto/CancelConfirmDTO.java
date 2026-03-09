package com.stocktrade.matchingsystem.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 撤单确认回报 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelConfirmDTO {

    /** 撤单请求编号 */
    private String clOrderId;

    /** 被撤原始订单编号 */
    private String origClOrderId;

    private String market;
    private String securityId;
    private String shareHolderId;
    private String side;
    private Integer qty;
    private Double price;

    /** 累计已成交数量 */
    private Integer cumQty;

    /** 本次被撤销数量 */
    private Integer canceledQty;
}
