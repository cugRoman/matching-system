package com.stocktrade.matchingsystem.report;

import com.stocktrade.matchingsystem.service.OrderProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 回报分发器 — 将处理结果推送给前端 (通过 WebSocket)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportDispatcher {

    private final WebSocketPublisher webSocketPublisher;

    /**
     * 分发处理结果
     */
    public void dispatch(OrderProcessResult result) {
        switch (result.getType()) {
            case REJECTED_INVALID -> {
                log.info("Dispatching reject report: {}", result.getRejectReport().getClOrderId());
                webSocketPublisher.publishReject(result.getRejectReport());
            }
            case ACCEPTED -> {
                log.info("Dispatching confirm report: {}", result.getConfirmReport().getClOrderId());
                webSocketPublisher.publishConfirm(result.getConfirmReport());
                result.getExecReports().forEach(webSocketPublisher::publishExecution);
                result.getCancelConfirms().forEach(webSocketPublisher::publishCancelConfirm);
            }
            case REJECTED_WASH -> {
                log.warn("Dispatching wash trade rejection");
                if (result.getConfirmReport() != null) {
                    webSocketPublisher.publishConfirm(result.getConfirmReport());
                }
                if (result.getRejectReport() != null) {
                    webSocketPublisher.publishWashTradeAlert(result.getRejectReport());
                }
                result.getCancelConfirms().forEach(webSocketPublisher::publishCancelConfirm);
            }
        }
    }
}
