package com.stocktrade.matchingsystem.validator;

import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.model.dto.RejectReportDTO;

import java.util.Optional;

/**
 * 校验规则接口 — 每条校验规则实现此接口（责任链模式）
 */
public interface ValidationRule {

    /**
     * 校验订单
     * 
     * @return empty = 校验通过; present = 校验失败，返回拒绝回报
     */
    Optional<RejectReportDTO> validate(OrderDTO order);
}
