package com.stocktrade.matchingsystem.exchange;

import com.stocktrade.matchingsystem.common.model.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Level-1 交易所模拟器：
 * - 收到订单后立即确认
 * - 收到撤单后立即回执
 */
@Slf4j
@Service
public class ExchangeSimulator {

    /** 接收订单并立即返回确认。 */
    public ConfirmReportDTO receiveOrder(OrderDTO order) {
        log.info("[ExchangeSim] Received order: {} {} {} {} @ {}",
                order.getClOrderId(), order.getSide(), order.getSecurityId(),
                order.getQty(), order.getPrice());

        return ConfirmReportDTO.builder()
                .clOrderId(order.getClOrderId())
                .market(order.getMarket())
                .securityId(order.getSecurityId())
                .side(order.getSide())
                .qty(order.getQty())
                .price(order.getPrice())
                .shareHolderId(order.getShareHolderId())
                .build();
    }

    /** 接收撤单请求并立即返回全撤确认。 */
    public CancelConfirmDTO receiveCancelRequest(CancelRequestDTO cancel, OrderDTO originalOrder) {
        log.info("[ExchangeSim] Received cancel for: {}", cancel.getOrigClientId());

        int canceledQty = originalOrder != null ? originalOrder.getQty() : 0;

        return CancelConfirmDTO.builder()
                .clOrderId(cancel.getOrderId())
                .origClOrderId(cancel.getOrigClientId())
                .market(cancel.getMarket())
                .securityId(cancel.getSecurityId())
                .shareHolderId(cancel.getShareHolderId())
                .side(cancel.getSide())
                .qty(originalOrder != null ? originalOrder.getQty() : 0)
                .price(originalOrder != null ? originalOrder.getPrice() : 0.0)
                .cumQty(0)
                .canceledQty(canceledQty)
                .build();
    }
}

