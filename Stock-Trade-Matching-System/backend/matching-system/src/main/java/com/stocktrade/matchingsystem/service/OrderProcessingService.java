package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.common.model.InternalOrder;
import com.stocktrade.matchingsystem.common.model.dto.CancelConfirmDTO;
import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * v1 兼容入口包装层。
 * 业务流程统一委托给 OrchestratorService。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final OrchestratorService orchestratorService;

    /** 处理一笔新订单。 */
    public OrderProcessResult processOrder(OrderDTO orderDTO) {
        return orchestratorService.processOrder(orderDTO);
    }

    /** 查询全部订单。 */
    public List<InternalOrder> getAllOrders() {
        return orchestratorService.getAllOrders();
    }

    /** 按委托号查询单笔订单。 */
    public Optional<InternalOrder> getOrder(String clOrderId) {
        return orchestratorService.getOrder(clOrderId);
    }

    public List<CancelConfirmDTO> expireOrders(LocalDate todayTradeDate) {
        return orchestratorService.expireOrders(todayTradeDate);
    }
}

