package com.stocktrade.matchingsystem.matching;

import com.stocktrade.matchingsystem.common.model.InternalOrder;
import com.stocktrade.matchingsystem.common.model.dto.ExecReportDTO;
import com.stocktrade.matchingsystem.common.util.IdGenerator;
import com.stocktrade.matchingsystem.risk.OrderIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 内部撮合引擎 — 按价格优先、时间优先撮合
 *
 * 基础定价策略: 取被动方(挂单方)价格
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingEngine {

    private final OrderIndex orderIndex;

    /**
     * 执行内部撮合: incoming 订单 与 counterOrders 列表逐一配对
     *
     * @param incoming      后到的订单 (对敲触发方)
     * @param counterOrders 可配对的对手方订单列表 (已按价格优先排序)
     * @return 撮合结果
     */
    public MatchResult match(InternalOrder incoming, List<InternalOrder> counterOrders) {
        List<ExecReportDTO> reports = new ArrayList<>();
        List<InternalOrder> matchedCounters = new ArrayList<>();
        int remainQty = incoming.getQtyRemaining();

        for (InternalOrder counter : counterOrders) {
            if (remainQty <= 0)
                break;
            if (counter.getQtyRemaining() <= 0)
                continue;

            // 成交数量 = min(incoming剩余, counter剩余)
            int theoretical = Math.min(remainQty, counter.getQtyRemaining());
            int execQty = (theoretical / 100) * 100;
            if (execQty <= 0) {
                break;
            }

            // 成交价 = 先到方 (counter) 的价格 — 被动方报价原则
            double execPrice = counter.getPrice();

            // 生成唯一成交编号 (买方和卖方共享同一个execId)
            String execId = IdGenerator.nextExecId();

            // 买方成交回报
            InternalOrder buyOrder = incoming.getSide().getCode().equals("B") ? incoming : counter;
            InternalOrder sellOrder = incoming.getSide().getCode().equals("B") ? counter : incoming;

            reports.add(buildExecReport(buyOrder, execId, execQty, execPrice));
            reports.add(buildExecReport(sellOrder, execId, execQty, execPrice));

            // 更新剩余数量
            remainQty -= execQty;
            incoming.setQtyRemaining(remainQty);
            incoming.setQtyFilled(incoming.getQtyFilled() + execQty);

            counter.setQtyRemaining(counter.getQtyRemaining() - execQty);
            counter.setQtyFilled(counter.getQtyFilled() + execQty);

            // 若 counter 全部成交，从索引移除
            if (counter.getQtyRemaining() <= 0) {
                orderIndex.removeOrder(counter);
            }

            log.info("Internal match: {} vs {} => {} shares @ {}",
                    incoming.getClOrderId(), counter.getClOrderId(), execQty, execPrice);
            matchedCounters.add(counter);
        }

        return new MatchResult(reports, matchedCounters, remainQty);
    }

    private ExecReportDTO buildExecReport(InternalOrder order, String execId, int execQty, double execPrice) {
        return ExecReportDTO.builder()
                .clOrderId(order.getClOrderId())
                .market(order.getMarket())
                .securityId(order.getSecurityId())
                .side(order.getSide().getCode())
                .qty(order.getQty())
                .price(order.getPrice())
                .shareHolderId(order.getShareHolderId())
                .execId(execId)
                .execQty(execQty)
                .execPrice(execPrice)
                .execSource("INTERNAL")
                .build();
    }
}
