package com.stocktrade.matchingsystem.validator.rules;

import com.stocktrade.matchingsystem.common.constants.OrderSide;
import com.stocktrade.matchingsystem.common.constants.RejectCode;
import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.model.dto.RejectReportDTO;
import com.stocktrade.matchingsystem.validator.ValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 买卖方向校验: B 或 S
 */
@Component
@Order(4)
public class SideRule implements ValidationRule {

    @Override
    public Optional<RejectReportDTO> validate(OrderDTO order) {
        if (order.getSide() == null || !OrderSide.isValid(order.getSide())) {
            return Optional.of(RejectReportDTO.builder()
                    .clOrderId(order.getClOrderId())
                    .market(order.getMarket())
                    .securityId(order.getSecurityId())
                    .side(order.getSide())
                    .qty(order.getQty())
                    .price(order.getPrice())
                    .shareHolderId(order.getShareHolderId())
                    .rejectCode(RejectCode.INVALID_SIDE.getCode())
                    .rejectText("side must be B (Buy) or S (Sell)")
                    .build());
        }
        return Optional.empty();
    }
}
