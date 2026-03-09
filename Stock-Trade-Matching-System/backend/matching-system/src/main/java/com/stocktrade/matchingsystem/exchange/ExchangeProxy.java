package com.stocktrade.matchingsystem.exchange;

import com.stocktrade.matchingsystem.common.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 交易所代理适配层。
 * 维护已外送订单的内存登记表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeProxy {

    private final ExchangeSimulator exchangeSimulator;

    /** 已外送到交易所的订单。 */
    private final ConcurrentHashMap<String, OrderDTO> sentOrders = new ConcurrentHashMap<>();

    /** 外送订单并登记为已发送。 */
    public ConfirmReportDTO forwardOrder(OrderDTO order) {
        sentOrders.put(order.getClOrderId(), order);
        log.info("Forwarding order {} to exchange", order.getClOrderId());
        return exchangeSimulator.receiveOrder(order);
    }

    /** 为已外送订单发送撤单请求。 */
    public CancelConfirmDTO sendCancelRequest(String origOrderId, String cancelId) {
        OrderDTO original = sentOrders.get(origOrderId);
        if (original == null) {
            log.warn("Cannot cancel order {}: not found in sent registry", origOrderId);
            return null;
        }

        CancelRequestDTO cancelReq = CancelRequestDTO.builder()
                .orderId(cancelId)
                .origClientId(origOrderId)
                .market(original.getMarket())
                .securityId(original.getSecurityId())
                .shareHolderId(original.getShareHolderId())
                .side(original.getSide())
                .build();

        CancelConfirmDTO result = exchangeSimulator.receiveCancelRequest(cancelReq, original);
        sentOrders.remove(origOrderId);
        return result;
    }

    /** 查询登记表中的已外送订单。 */
    public OrderDTO getSentOrder(String clOrderId) {
        return sentOrders.get(clOrderId);
    }

    /** 检查订单是否已外送到交易所。 */
    public boolean isOrderSent(String clOrderId) {
        return sentOrders.containsKey(clOrderId);
    }

    public void removeSentOrder(String clOrderId) {
        sentOrders.remove(clOrderId);
    }
}

