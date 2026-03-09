package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.common.model.InternalOrder;
import com.stocktrade.matchingsystem.common.model.dto.CancelConfirmDTO;
import com.stocktrade.matchingsystem.common.model.dto.ConfirmReportDTO;
import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.util.IdGenerator;
import com.stocktrade.matchingsystem.exchange.ExchangeProxy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 交易所服务门面，封装外送与撤单操作。
 */
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeProxy exchangeProxy;

    public ConfirmReportDTO forward(OrderDTO order) {
        return exchangeProxy.forwardOrder(order);
    }

    public CancelConfirmDTO cancel(InternalOrder order) {
        String cancelId = IdGenerator.nextSystemCancelId();
        return exchangeProxy.sendCancelRequest(order.getClOrderId(), cancelId);
    }

    public boolean isSent(String clOrderId) {
        return exchangeProxy.isOrderSent(clOrderId);
    }

    public void removeSent(String clOrderId) {
        exchangeProxy.removeSentOrder(clOrderId);
    }
}

