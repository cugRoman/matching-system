package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.common.constants.OrderStatus;
import com.stocktrade.matchingsystem.common.constants.RejectCode;
import com.stocktrade.matchingsystem.common.model.InternalOrder;
import com.stocktrade.matchingsystem.common.model.dto.*;
import com.stocktrade.matchingsystem.common.model.entity.ExecutionEntity;
import com.stocktrade.matchingsystem.common.model.entity.OrderEntity;
import com.stocktrade.matchingsystem.common.model.entity.ReportEntity;
import com.stocktrade.matchingsystem.common.util.IdGenerator;
import com.stocktrade.matchingsystem.matching.MatchResult;
import com.stocktrade.matchingsystem.persistence.repository.ExecutionRepository;
import com.stocktrade.matchingsystem.persistence.repository.OrderRepository;
import com.stocktrade.matchingsystem.persistence.repository.ReportRepository;
import com.stocktrade.matchingsystem.risk.OrderIndex;
import com.stocktrade.matchingsystem.risk.WashTradeResult;
import com.stocktrade.matchingsystem.validator.OrderValidationChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * 订单编排服务：
 * 负责校验、风控、撮合、外送/撤单与落库主流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrchestratorService {

    private final OrderValidationChain validationChain;
    private final RiskService riskService;
    private final MatchService matchService;
    private final ExchangeService exchangeService;
    private final OrderIndex orderIndex;
    private final OrderRepository orderRepository;
    private final ExecutionRepository executionRepository;
    private final ReportRepository reportRepository;
    private final PositionService positionService; 

    private final Map<String, InternalOrder> orderRegistry = new LinkedHashMap<>();

    public OrderProcessResult processOrder(OrderDTO orderDTO) {
        log.info("Processing order: {} {} {} {} @ {}",
                orderDTO.getClOrderId(), orderDTO.getSide(), orderDTO.getSecurityId(),
                orderDTO.getQty(), orderDTO.getPrice());

        Optional<RejectReportDTO> validationResult = validationChain.validate(orderDTO);
        if (validationResult.isPresent()) {
            RejectReportDTO reject = validationResult.get();
            persistReport(buildRejectReportEntity(reject, "REJECT"));
            return OrderProcessResult.rejected(reject);
        }

         // 判断如果是卖单（假设 "S" 代表卖出 Sell，如果是 "B" 代表卖出请自行修改）
        if ("S".equalsIgnoreCase(orderDTO.getSide())) {
            boolean freezeSuccess = positionService.freezePositionForSell(
                    orderDTO.getShareHolderId(),
                    orderDTO.getMarket(),
                    orderDTO.getSecurityId(),
                    orderDTO.getQty()
            );

            // 如果冻结失败（持仓不足）
            if (!freezeSuccess) {
                log.warn("持仓不足，拒绝订单: {}", orderDTO.getClOrderId());
                RejectReportDTO posReject = RejectReportDTO.builder()
                        .clOrderId(orderDTO.getClOrderId())
                        .market(orderDTO.getMarket())
                        .securityId(orderDTO.getSecurityId())
                        .side(orderDTO.getSide())
                        .qty(orderDTO.getQty())
                        .price(orderDTO.getPrice())
                        .shareHolderId(orderDTO.getShareHolderId())
                        .rejectCode(RejectCode.INSUFFICIENT_POSITION.getCode())
                        .rejectText(RejectCode.INSUFFICIENT_POSITION.getText())
                        .build();
                persistReport(buildRejectReportEntity(posReject, "REJECT"));
                return OrderProcessResult.rejected(posReject); // 拒绝订单并返回
            }
        }

        InternalOrder order = InternalOrder.fromDTO(orderDTO);
        order.setStatus(OrderStatus.ACTIVE_IN_POOL);
        persistOrder(order);
        orderRegistry.put(order.getClOrderId(), order);

        ConfirmReportDTO confirm = buildConfirmReport(orderDTO);
        persistReport(buildConfirmReportEntity(confirm));

        String washKey = order.washTradeKey();
        String matchKey = order.matchKey();

        synchronized (orderIndex.getWashLock(washKey)) {
            synchronized (orderIndex.getMatchLock(matchKey)) {
                WashTradeResult washResult = riskService.detectWash(order);
                if (washResult.isDetected()) {

                    if ("S".equalsIgnoreCase(orderDTO.getSide())) {
                        positionService.handleCancel(orderDTO.getShareHolderId(), orderDTO.getSecurityId(), "S", orderDTO.getQty());
                    }

                    order.setStatus(OrderStatus.REJECTED_WASH);
                    order.setWashTrade(true);
                    persistOrder(order);

                    RejectReportDTO washReject = RejectReportDTO.builder()
                            .clOrderId(orderDTO.getClOrderId())
                            .market(orderDTO.getMarket())
                            .securityId(orderDTO.getSecurityId())
                            .side(orderDTO.getSide())
                            .qty(orderDTO.getQty())
                            .price(orderDTO.getPrice())
                            .shareHolderId(orderDTO.getShareHolderId())
                            .rejectCode(RejectCode.WASH_TRADE_DETECTED.getCode())
                            .rejectText(RejectCode.WASH_TRADE_DETECTED.getText())
                            .build();
                    persistReport(buildRejectReportEntity(washReject, "REJECT"));

                    List<CancelConfirmDTO> cancelConfirms = handleExchangeCancel(washResult.getCounterOrders());
                    return OrderProcessResult.washRejected(confirm, washReject, cancelConfirms);
                }

                MatchResult matchResult = matchService.match(order);
                persistExecutions(matchResult.getExecReports());

                List<CancelConfirmDTO> cancelConfirms = handleExchangeCancel(matchResult.getMatchedCounters());

                if (order.getQtyRemaining() <= 0) {
                    order.setStatus(OrderStatus.FILLED);
                } else {
                    order.setStatus(OrderStatus.EXCH_WORKING);
                    order.setSentToExchange(true);
                    exchangeService.forward(orderDTO);
                    orderIndex.addOrder(order);
                }
                persistOrder(order);

                return OrderProcessResult.accepted(confirm, matchResult.getExecReports(), cancelConfirms);
            }
        }
    }

    public List<InternalOrder> getAllOrders() {
        return new ArrayList<>(orderRegistry.values());
    }

    public Optional<InternalOrder> getOrder(String clOrderId) {
        return Optional.ofNullable(orderRegistry.get(clOrderId));
    }

    public List<CancelConfirmDTO> expireOrders(LocalDate todayTradeDate) {
        List<CancelConfirmDTO> results = new ArrayList<>();
        for (InternalOrder order : orderRegistry.values()) {
            if (order.getStatus() == OrderStatus.EXCH_WORKING
                    && order.getTradeDate().isBefore(todayTradeDate)) {
                order.setStatus(OrderStatus.CANCELED);
                order.setSentToExchange(false);
                exchangeService.removeSent(order.getClOrderId());
                orderIndex.removeOrder(order);
                persistOrder(order);

                CancelConfirmDTO cancel = CancelConfirmDTO.builder()
                        .clOrderId(IdGenerator.nextSystemCancelId())
                        .origClOrderId(order.getClOrderId())
                        .market(order.getMarket())
                        .securityId(order.getSecurityId())
                        .shareHolderId(order.getShareHolderId())
                        .side(order.getSide().getCode())
                        .qty(order.getQty())
                        .price(order.getPrice())
                        .cumQty(order.getQtyFilled())
                        .canceledQty(order.getQtyRemaining())
                        .build();
                results.add(cancel);
                persistReport(buildCancelReportEntity(cancel,
                        "CANCEL_ACK",
                        "Auto expired at end of trading day"));
            }
        }
        return results;
    }

    private List<CancelConfirmDTO> handleExchangeCancel(List<InternalOrder> counters) {
        List<CancelConfirmDTO> cancelConfirms = new ArrayList<>();
        for (InternalOrder counter : counters) {
            if (counter.isSentToExchange() && counter.getStatus() == OrderStatus.EXCH_WORKING) {
                counter.setStatus(OrderStatus.CANCEL_REQUESTED);
                CancelConfirmDTO cancelConfirm = exchangeService.cancel(counter);
                if (cancelConfirm != null) {
                    cancelConfirms.add(cancelConfirm);
                    counter.setStatus(OrderStatus.CANCELED);
                    counter.setSentToExchange(false);
                    exchangeService.removeSent(counter.getClOrderId());
                    orderIndex.removeOrder(counter);
                    persistOrder(counter);
                    persistReport(buildCancelReportEntity(cancelConfirm, "CANCEL_ACK", null));

                     positionService.handleCancel(
                            cancelConfirm.getShareHolderId(), 
                            cancelConfirm.getSecurityId(), 
                            cancelConfirm.getSide(), 
                            cancelConfirm.getCanceledQty()
                    );
                }
            }
            if (counter.getQtyRemaining() <= 0 && counter.getStatus() != OrderStatus.CANCELED) {
                counter.setStatus(OrderStatus.FILLED);
                orderIndex.removeOrder(counter);
            }
            persistOrder(counter);
        }
        return cancelConfirms;
    }

    private ConfirmReportDTO buildConfirmReport(OrderDTO orderDTO) {
        return ConfirmReportDTO.builder()
                .clOrderId(orderDTO.getClOrderId())
                .market(orderDTO.getMarket())
                .securityId(orderDTO.getSecurityId())
                .side(orderDTO.getSide())
                .qty(orderDTO.getQty())
                .price(orderDTO.getPrice())
                .shareHolderId(orderDTO.getShareHolderId())
                .build();
    }

    private void persistOrder(InternalOrder order) {
        OrderEntity entity = OrderEntity.builder()
                .clOrderId(order.getClOrderId())
                .market(order.getMarket())
                .securityId(order.getSecurityId())
                .side(order.getSide().getCode())
                .qty(order.getQty())
                .price(order.getPrice())
                .shareHolderId(order.getShareHolderId())
                .tradeDate(order.getTradeDate())
                .status(order.getStatus().name())
                .sentToExchange(order.isSentToExchange())
                .qtyFilled(order.getQtyFilled())
                .qtyRemaining(order.getQtyRemaining())
                .washTrade(order.isWashTrade())
                .recvTime(order.getReceivedAt())
                .updatedAt(Instant.now())
                .build();
        orderRepository.findByClOrderId(order.getClOrderId())
                .ifPresent(existing -> entity.setId(existing.getId()));
        orderRepository.save(entity);
    }

    private void persistExecutions(List<ExecReportDTO> execReports) {
        for (ExecReportDTO report : execReports) {
            ExecutionEntity entity = ExecutionEntity.builder()
                    .execId(report.getExecId())
                    .clOrderId(report.getClOrderId())
                    .market(report.getMarket())
                    .securityId(report.getSecurityId())
                    .side(report.getSide())
                    .execQty(report.getExecQty())
                    .execPrice(report.getExecPrice())
                    .shareHolderId(report.getShareHolderId())
                    .execType(report.getExecSource() != null ? report.getExecSource() : "INTERNAL")
                    .counterOrderId(null)
                    .build();
            executionRepository.save(entity);
            persistReport(buildExecReportEntity(report));

            positionService.handleExecution(
                    report.getShareHolderId(),
                    report.getMarket(),
                    report.getSecurityId(),
                    report.getSide(),
                    report.getExecQty()
            );
        }
    }

    private void persistReport(ReportEntity report) {
        reportRepository.save(report);
    }

    private ReportEntity buildConfirmReportEntity(ConfirmReportDTO report) {
        return ReportEntity.builder()
                .reportId(IdGenerator.nextReportId())
                .type("CONFIRM")
                .refClOrdId(report.getClOrderId())
                .market(report.getMarket())
                .securityId(report.getSecurityId())
                .side(report.getSide())
                .qty(report.getQty())
                .price(report.getPrice())
                .shareHolderId(report.getShareHolderId())
                .build();
    }

    private ReportEntity buildRejectReportEntity(RejectReportDTO report, String type) {
        return ReportEntity.builder()
                .reportId(IdGenerator.nextReportId())
                .type(type)
                .refClOrdId(report.getClOrderId())
                .market(report.getMarket())
                .securityId(report.getSecurityId())
                .side(report.getSide())
                .qty(report.getQty())
                .price(report.getPrice())
                .shareHolderId(report.getShareHolderId())
                .rejectCode(report.getRejectCode())
                .rejectText(report.getRejectText())
                .build();
    }

    private ReportEntity buildExecReportEntity(ExecReportDTO report) {
        return ReportEntity.builder()
                .reportId(IdGenerator.nextReportId())
                .type("EXECUTION")
                .refClOrdId(report.getClOrderId())
                .market(report.getMarket())
                .securityId(report.getSecurityId())
                .side(report.getSide())
                .qty(report.getQty())
                .price(report.getPrice())
                .shareHolderId(report.getShareHolderId())
                .execId(report.getExecId())
                .execQty(report.getExecQty())
                .execPrice(report.getExecPrice())
                .execSource(report.getExecSource())
                .build();
    }

    private ReportEntity buildCancelReportEntity(CancelConfirmDTO report, String type, String cancelText) {
        return ReportEntity.builder()
                .reportId(IdGenerator.nextReportId())
                .type(type)
                .refClOrdId(report.getOrigClOrderId())
                .market(report.getMarket())
                .securityId(report.getSecurityId())
                .side(report.getSide())
                .qty(report.getQty())
                .price(report.getPrice())
                .shareHolderId(report.getShareHolderId())
                .origClOrdId(report.getOrigClOrderId())
                .cancelText(cancelText)
                .canceledQty(report.getCanceledQty())
                .build();
    }
}

