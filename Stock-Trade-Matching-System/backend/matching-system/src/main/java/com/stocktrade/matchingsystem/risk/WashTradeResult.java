package com.stocktrade.matchingsystem.risk;

import com.stocktrade.matchingsystem.common.model.InternalOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 对敲检测结果
 */
@Data
@AllArgsConstructor
public class WashTradeResult {

    /** 是否检测到对敲 */
    private final boolean detected;

    /** 可配对的对手方订单列表 (按价格优先排序) */
    private final List<InternalOrder> counterOrders;

    public static WashTradeResult notDetected() {
        return new WashTradeResult(false, List.of());
    }

    public static WashTradeResult detected(List<InternalOrder> counterOrders) {
        return new WashTradeResult(true, counterOrders);
    }
}
