package com.stocktrade.matchingsystem.validator.rules;

import com.stocktrade.matchingsystem.common.constants.RejectCode;
import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.model.dto.RejectReportDTO;
import com.stocktrade.matchingsystem.validator.ValidationRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 股票代码校验: 6位数字字符
 */
@Component
@Order(3)
public class SecurityIdRule implements ValidationRule {

    private static final String PATTERN = "^\\d{6}$";

    @Override
    public Optional<RejectReportDTO> validate(OrderDTO order) {
        if (order.getSecurityId() == null || !order.getSecurityId().matches(PATTERN)) {
            return Optional.of(RejectReportDTO.builder()
                    .clOrderId(order.getClOrderId())
                    .market(order.getMarket())
                    .securityId(order.getSecurityId())
                    .side(order.getSide())
                    .qty(order.getQty())
                    .price(order.getPrice())
                    .shareHolderId(order.getShareHolderId())
                    .rejectCode(RejectCode.INVALID_SECURITY_ID.getCode())
                    .rejectText("securityId must be 6 digits")
                    .build());
        }
        return Optional.empty();
    }
}
