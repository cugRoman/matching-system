package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.common.model.dto.ExecutionDetailItemDTO;
import com.stocktrade.matchingsystem.common.model.dto.OrderExecutionDetailDTO;
import com.stocktrade.matchingsystem.common.model.dto.UserOrderSummaryDTO;
import com.stocktrade.matchingsystem.common.model.entity.ExecutionEntity;
import com.stocktrade.matchingsystem.common.model.entity.OrderEntity;
import com.stocktrade.matchingsystem.persistence.repository.ExecutionRepository;
import com.stocktrade.matchingsystem.persistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserOrderQueryService {

    private final OrderRepository orderRepository;
    private final ExecutionRepository executionRepository;

    public List<UserOrderSummaryDTO> listByShareHolder(String shareHolderId) {
        return orderRepository.findByShareHolderIdOrderByRecvTimeDesc(shareHolderId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public Optional<OrderExecutionDetailDTO> getOrderExecutionDetail(String shareHolderId, String clOrderId) {
        Optional<OrderEntity> orderOpt = orderRepository.findByClOrderId(clOrderId)
                .filter(order -> Objects.equals(order.getShareHolderId(), shareHolderId));
        if (orderOpt.isEmpty()) {
            return Optional.empty();
        }

        OrderEntity order = orderOpt.get();
        List<ExecutionEntity> executions = executionRepository.findByClOrderIdOrderByCreatedAtDescIdDesc(clOrderId);

        int totalExecutedQty = executions.stream()
                .map(ExecutionEntity::getExecQty)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        double totalAmount = executions.stream()
                .filter(e -> e.getExecQty() != null && e.getExecPrice() != null)
                .mapToDouble(e -> e.getExecQty() * e.getExecPrice())
                .sum();

        Double avgExecutedPrice = totalExecutedQty > 0 ? totalAmount / totalExecutedQty : null;

        return Optional.of(OrderExecutionDetailDTO.builder()
                .clOrderId(order.getClOrderId())
                .market(order.getMarket())
                .securityId(order.getSecurityId())
                .side(order.getSide())
                .qty(order.getQty())
                .qtyFilled(order.getQtyFilled())
                .qtyRemaining(order.getQtyRemaining())
                .status(order.getStatus())
                .executionCount(executions.size())
                .totalExecutedQty(totalExecutedQty)
                .avgExecutedPrice(avgExecutedPrice)
                .lastExecutedAt(executions.isEmpty() ? null : executions.get(0).getCreatedAt())
                .executions(executions.stream().map(this::toExecutionItem).toList())
                .build());
    }

    private UserOrderSummaryDTO toSummary(OrderEntity order) {
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

    private ExecutionDetailItemDTO toExecutionItem(ExecutionEntity execution) {
        return ExecutionDetailItemDTO.builder()
                .execId(execution.getExecId())
                .execQty(execution.getExecQty())
                .execPrice(execution.getExecPrice())
                .execType(execution.getExecType())
                .counterOrderId(execution.getCounterOrderId())
                .executedAt(execution.getCreatedAt())
                .build();
    }
}
