package com.stocktrade.matchingsystem.validator.rules;

import com.stocktrade.matchingsystem.common.constants.MarketCode;
import com.stocktrade.matchingsystem.common.constants.OrderSide;
import com.stocktrade.matchingsystem.common.constants.RejectCode;
import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.model.dto.RejectReportDTO;
import com.stocktrade.matchingsystem.validator.ValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 市场代码校验
 */
@Component
@Order(2)
public class MarketRule implements ValidationRule {

    @Override
    public Optional<RejectReportDTO> validate(OrderDTO order) {
        if (order.getMarket() == null || !MarketCode.isValid(order.getMarket())) {
            return Optional.of(RejectReportDTO.builder()
                    .clOrderId(order.getClOrderId())
                    .market(order.getMarket())
                    .securityId(order.getSecurityId())
                    .side(order.getSide())
                    .qty(order.getQty())
                    .price(order.getPrice())
                    .shareHolderId(order.getShareHolderId())
                    .rejectCode(RejectCode.INVALID_MARKET.getCode())
                    .rejectText("market must be XSHG, XSHE, or BJSE")
                    .build());
        }
        return Optional.empty();
    }
}
