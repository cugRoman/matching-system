package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.common.model.InternalOrder;
import com.stocktrade.matchingsystem.risk.WashTradeDetector;
import com.stocktrade.matchingsystem.risk.WashTradeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 风控服务 — 对敲检测
 */
@Service
@RequiredArgsConstructor
public class RiskService {

    private final WashTradeDetector washTradeDetector;

    public WashTradeResult detectWash(InternalOrder incoming) {
        return washTradeDetector.detect(incoming);
    }
}
