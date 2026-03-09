package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.common.model.InternalOrder;
import com.stocktrade.matchingsystem.matching.MatchResult;
import com.stocktrade.matchingsystem.matching.MatchingEngine;
import com.stocktrade.matchingsystem.risk.OrderIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内部撮合服务入口。
 */
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchingEngine matchingEngine;
    private final OrderIndex orderIndex;

    public MatchResult match(InternalOrder incoming) {
        List<InternalOrder> counters = orderIndex.findMatchCounterOrders(incoming);
        return matchingEngine.match(incoming, counters);
    }
}

