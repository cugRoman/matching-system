package com.stocktrade.matchingsystem.validator;

import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.model.dto.RejectReportDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 订单校验责任链 — 串联全部校验规则，按顺序执行，首个失败即短路返回
 */
@Component
public class OrderValidationChain {

    private final List<ValidationRule> rules;

    public OrderValidationChain(List<ValidationRule> rules) {
        this.rules = rules;
    }

    /**
     * 执行全部校验规则
     * 
     * @return empty = 全部通过; present = 首个失败的拒绝回报
     */
    public Optional<RejectReportDTO> validate(OrderDTO order) {
        for (ValidationRule rule : rules) {
            Optional<RejectReportDTO> result = rule.validate(order);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
