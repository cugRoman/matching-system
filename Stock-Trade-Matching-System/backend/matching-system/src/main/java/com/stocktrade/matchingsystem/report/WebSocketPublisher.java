package com.stocktrade.matchingsystem.report;

import com.stocktrade.matchingsystem.common.model.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * WebSocket 消息发布器
 *
 * 通过 STOMP 协议向前端推送各类回报:
 * - /topic/reports/confirm 确认回报
 * - /topic/reports/reject 拒绝回报
 * - /topic/reports/execution 成交回报
 * - /topic/alerts/washtrade 对敲警报
 * - /topic/reports/cancel 撤单确认
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishConfirm(ConfirmReportDTO report) {
        messagingTemplate.convertAndSend("/topic/reports/confirm", report);
        log.debug("Published confirm report for {}", report.getClOrderId());
    }

    public void publishReject(RejectReportDTO report) {
        messagingTemplate.convertAndSend("/topic/reports/reject", report);
        log.debug("Published reject report for {}", report.getClOrderId());
    }

    public void publishExecution(ExecReportDTO report) {
        messagingTemplate.convertAndSend("/topic/reports/execution", report);
        log.debug("Published execution report: {} qty={} @ {}", report.getExecId(),
                report.getExecQty(), report.getExecPrice());
    }

    public void publishWashTradeAlert(RejectReportDTO report) {
        messagingTemplate.convertAndSend("/topic/alerts/washtrade", report);
        log.warn("Published wash trade alert for {}", report.getClOrderId());
    }

    public void publishCancelConfirm(CancelConfirmDTO report) {
        messagingTemplate.convertAndSend("/topic/reports/cancel", report);
        log.debug("Published cancel confirm for {}", report.getOrigClOrderId());
    }
}
