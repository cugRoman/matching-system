package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.common.model.dto.UserOrderSummaryDTO;
import com.stocktrade.matchingsystem.common.model.dto.UserReportSummaryDTO;
import com.stocktrade.matchingsystem.common.model.dto.UserStockActivityDTO;
import com.stocktrade.matchingsystem.common.model.entity.OrderEntity;
import com.stocktrade.matchingsystem.common.model.entity.ReportEntity;
import com.stocktrade.matchingsystem.persistence.repository.OrderRepository;
import com.stocktrade.matchingsystem.persistence.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final OrderRepository orderRepository;
    private final ReportRepository reportRepository;

    public List<UserStockActivityDTO> listByShareHolder(String shareHolderId) {
        List<OrderEntity> orders = orderRepository.findByShareHolderIdOrderByRecvTimeDesc(shareHolderId);
        List<ReportEntity> reports = reportRepository.findByShareHolderIdOrderByTsDesc(shareHolderId);

        Map<SymbolKey, List<UserOrderSummaryDTO>> ordersBySymbol = new LinkedHashMap<>();
        Map<SymbolKey, List<UserReportSummaryDTO>> reportsBySymbol = new LinkedHashMap<>();

        for (OrderEntity order : orders) {
            SymbolKey key = new SymbolKey(order.getMarket(), order.getSecurityId());
            ordersBySymbol.computeIfAbsent(key, k -> new ArrayList<>()).add(toOrderSummary(order));
        }

        for (ReportEntity report : reports) {
            if (isBlank(report.getMarket()) || isBlank(report.getSecurityId())) {
                continue;
            }
            SymbolKey key = new SymbolKey(report.getMarket(), report.getSecurityId());
            reportsBySymbol.computeIfAbsent(key, k -> new ArrayList<>()).add(toReportSummary(report));
        }

        List<SymbolKey> allKeys = new ArrayList<>();
        allKeys.addAll(ordersBySymbol.keySet());
        for (SymbolKey key : reportsBySymbol.keySet()) {
            if (!allKeys.contains(key)) {
                allKeys.add(key);
            }
        }
        allKeys.sort(Comparator.comparing(SymbolKey::market).thenComparing(SymbolKey::securityId));

        List<UserStockActivityDTO> result = new ArrayList<>();
        for (SymbolKey key : allKeys) {
            List<UserOrderSummaryDTO> perStockOrders = ordersBySymbol.getOrDefault(key, List.of());
            List<UserReportSummaryDTO> perStockReports = reportsBySymbol.getOrDefault(key, List.of());

            int totalOrderQty = perStockOrders.stream()
                    .map(UserOrderSummaryDTO::getQty)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            int totalExecQty = perStockReports.stream()
                    .map(UserReportSummaryDTO::getExecQty)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            result.add(UserStockActivityDTO.builder()
                    .market(key.market())
                    .securityId(key.securityId())
                    .totalOrderQty(totalOrderQty)
                    .totalExecQty(totalExecQty)
                    .orders(perStockOrders)
                    .reports(perStockReports)
                    .build());
        }

        return result;
    }

    private UserOrderSummaryDTO toOrderSummary(OrderEntity order) {
        return UserOrderSummaryDTO.builder()
                .clOrderId(order.getClOrderId())
                .market(order.getMarket())
                .securityId(order.getSecurityId())
                .side(order.getSide())
                .qty(order.getQty())
                .price(order.getPrice())
                .status(order.getStatus())
                .qtyFilled(order.getQtyFilled())
                .qtyRemaining(order.getQtyRemaining())
                .recvTime(order.getRecvTime())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private UserReportSummaryDTO toReportSummary(ReportEntity report) {
        return UserReportSummaryDTO.builder()
                .reportId(report.getReportId())
                .type(report.getType())
                .refClOrdId(report.getRefClOrdId())
                .ts(report.getTs())
                .qty(report.getQty())
                .price(report.getPrice())
                .rejectCode(report.getRejectCode())
                .rejectText(report.getRejectText())
                .execId(report.getExecId())
                .execQty(report.getExecQty())
                .execPrice(report.getExecPrice())
                .execSource(report.getExecSource())
                .cancelText(report.getCancelText())
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SymbolKey(String market, String securityId) {
    }
}
