package com.stocktrade.matchingsystem.validator.rules;

import com.stocktrade.matchingsystem.common.constants.RejectCode;
import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.model.dto.RejectReportDTO;
import com.stocktrade.matchingsystem.validator.ValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * v1 数量校验：
 * qty 必须大于 0 且为 100 的整数倍。
 */
@Component
@Order(5)
public class QuantityRule implements ValidationRule {

    @Override
    public Optional<RejectReportDTO> validate(OrderDTO order) {
        if (order.getQty() == null || order.getQty() <= 0) {
            return Optional.of(buildReject(order, "qty must be greater than 0"));
        }
        if (order.getQty() % 100 != 0) {
            return Optional.of(buildReject(order, "qty must be a multiple of 100"));
        }
        return Optional.empty();
    }

    private RejectReportDTO buildReject(OrderDTO order, String reason) {
        return RejectReportDTO.builder()
                .clOrderId(order.getClOrderId())
                .market(order.getMarket())
                .securityId(order.getSecurityId())
                .side(order.getSide())
                .qty(order.getQty())
                .price(order.getPrice())
                .shareHolderId(order.getShareHolderId())
                .rejectCode(RejectCode.INVALID_QTY.getCode())
                .rejectText(reason)
                .build();
    }
}

