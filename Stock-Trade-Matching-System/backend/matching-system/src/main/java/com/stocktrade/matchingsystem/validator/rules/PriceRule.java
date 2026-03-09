package com.stocktrade.matchingsystem.validator.rules;

import com.stocktrade.matchingsystem.common.constants.RejectCode;
import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.model.dto.RejectReportDTO;
import com.stocktrade.matchingsystem.validator.ValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 价格校验: price > 0
 */
@Component
@Order(6)
public class PriceRule implements ValidationRule {

    @Override
    public Optional<RejectReportDTO> validate(OrderDTO order) {
        if (order.getPrice() == null || order.getPrice() <= 0) {
            return Optional.of(RejectReportDTO.builder()
                    .clOrderId(order.getClOrderId())
                    .market(order.getMarket())
                    .securityId(order.getSecurityId())
                    .side(order.getSide())
                    .qty(order.getQty())
                    .price(order.getPrice())
                    .shareHolderId(order.getShareHolderId())
                    .rejectCode(RejectCode.INVALID_PRICE.getCode())
                    .rejectText("price must be greater than 0")
                    .build());
        }
        return Optional.empty();
    }
}
