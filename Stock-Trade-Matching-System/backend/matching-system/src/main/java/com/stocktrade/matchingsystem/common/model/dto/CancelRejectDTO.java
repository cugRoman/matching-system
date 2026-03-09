package com.stocktrade.matchingsystem.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 撤单非法回报 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelRejectDTO {

    /** 撤单请求编号 */
    private String clOrderId;

    /** 待撤原始订单编号 */
    private String origClOrderId;

    /** 拒绝码 */
    private Integer rejectCode;

    /** 拒绝原因 */
    private String rejectText;
}
