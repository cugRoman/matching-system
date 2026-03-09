package com.stocktrade.matchingsystem.risk;

import com.stocktrade.matchingsystem.common.model.InternalOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对敲交易检测器
 *
 * 对敲成立条件:
 * 同一 tradeDate + shareHolderId + market + securityId + 方向相反 (一B一S)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WashTradeDetector {

    private final OrderIndex orderIndex;

    /**
     * 对新到订单执行对敲检测
     * 注意: 调用方应在 orderIndex.getLock(washTradeKey) 同步块内调用此方法
     */
    public WashTradeResult detect(InternalOrder incoming) {
        List<InternalOrder> counterOrders = orderIndex.findWashCounterOrders(incoming);

        if (counterOrders.isEmpty()) {
            log.debug("No wash trade detected for order {}", incoming.getClOrderId());
            return WashTradeResult.notDetected();
        }

        log.warn("WASH TRADE DETECTED: order {} ({} {} {} @ {}) has {} counter orders",
                incoming.getClOrderId(), incoming.getSide(), incoming.getSecurityId(),
                incoming.getQty(), incoming.getPrice(), counterOrders.size());

        return WashTradeResult.detected(counterOrders);
    }
}
