package com.stocktrade.matchingsystem.matching;

import com.stocktrade.matchingsystem.common.model.InternalOrder;
import com.stocktrade.matchingsystem.common.model.dto.ExecReportDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 撮合结果
 */
@Data
@AllArgsConstructor
public class MatchResult {

    /** 生成的全部成交回报 (买方+卖方各一份为一对) */
    private final List<ExecReportDTO> execReports;

    /** 被撮合到的对手方订单 (有成交则包含) */
    private final List<InternalOrder> matchedCounters;

    /** 发起方(incoming)剩余未成交数量 */
    private final int incomingRemainQty;
}
