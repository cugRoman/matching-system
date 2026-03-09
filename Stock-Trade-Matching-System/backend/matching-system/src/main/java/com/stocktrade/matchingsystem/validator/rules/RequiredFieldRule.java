package com.stocktrade.matchingsystem.validator.rules;

import com.stocktrade.matchingsystem.common.constants.RejectCode;
import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.model.dto.RejectReportDTO;
import com.stocktrade.matchingsystem.validator.ValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 必填字段校验: clOrderId, shareHolderId 非空
 */
@Component
@Order(1)
public class RequiredFieldRule implements ValidationRule {

    @Override
    public Optional<RejectReportDTO> validate(OrderDTO order) {
        if (order.getClOrderId() == null || order.getClOrderId().isBlank()) {
            return Optional.of(buildReject(order, RejectCode.INVALID_FIELD, "clOrderId is required"));
        }
        if (order.getShareHolderId() == null || order.getShareHolderId().isBlank()) {
            return Optional.of(buildReject(order, RejectCode.INVALID_FIELD, "shareHolderId is required"));
        }
        return Optional.empty();
    }

    private RejectReportDTO buildReject(OrderDTO order, RejectCode code, String detail) {
        return RejectReportDTO.builder()
                .clOrderId(order.getClOrderId())
                .market(order.getMarket())
                .securityId(order.getSecurityId())
                .side(order.getSide())
                .qty(order.getQty())
                .price(order.getPrice())
                .shareHolderId(order.getShareHolderId())
                .rejectCode(code.getCode())
                .rejectText(detail)
                .build();
    }
}
