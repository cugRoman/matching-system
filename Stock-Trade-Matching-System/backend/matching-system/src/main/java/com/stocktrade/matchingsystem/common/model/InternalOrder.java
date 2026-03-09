package com.stocktrade.matchingsystem.common.model;

import com.stocktrade.matchingsystem.common.constants.OrderSide;
import com.stocktrade.matchingsystem.common.constants.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 系统内部订单模型，用于风控、撮合与外送流程。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalOrder {

    private String clOrderId;
    private String shareHolderId;
    private LocalDate tradeDate;
    private String market;
    private String securityId;
    private OrderSide side;
    private int qty;
    private double price;

    /** 当前生命周期状态。 */
    @Builder.Default
    private OrderStatus status = OrderStatus.NEW;

    /** 是否已外送交易所。 */
    @Builder.Default
    private boolean sentToExchange = false;

    /** 累计成交数量。 */
    @Builder.Default
    private int qtyFilled = 0;

    /** 剩余未成交数量。 */
    private int qtyRemaining;

    /** 是否被标记为对敲相关订单。 */
    @Builder.Default
    private boolean washTrade = false;

    /** 接收时间。 */
    @Builder.Default
    private Instant receivedAt = Instant.now();

    /** 由 API DTO 构建内部订单对象。 */
    public static InternalOrder fromDTO(com.stocktrade.matchingsystem.common.model.dto.OrderDTO dto) {
        return InternalOrder.builder()
                .clOrderId(dto.getClOrderId())
                .shareHolderId(dto.getShareHolderId())
                .tradeDate(dto.getTradeDate() != null ? dto.getTradeDate() : LocalDate.now())
                .market(dto.getMarket())
                .securityId(dto.getSecurityId())
                .side(OrderSide.fromCode(dto.getSide()))
                .qty(dto.getQty())
                .price(dto.getPrice())
                .qtyRemaining(dto.getQty())
                .status(OrderStatus.NEW)
                .receivedAt(Instant.now())
                .build();
    }

    /** 对敲索引使用的键。 */
    public String washTradeKey() {
        return tradeDate + "|" + shareHolderId + "|" + market + "|" + securityId;
    }

    public String matchKey() {
        return market + "|" + securityId;
    }
}

