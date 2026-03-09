package com.stocktrade.matchingsystem.common.constants;

/**
 * 订单状态枚举 — v1 订单生命周期状态机
 */
public enum OrderStatus {
    NEW("新建"),
    REJECTED_INVALID("非法拒绝"),
    REJECTED_WASH("对敲拒绝"),
    ACTIVE_IN_POOL("池内可撮合"),
    EXCH_WORKING("交易所挂单中"),
    PARTIALLY_FILLED("部分成交"),
    FILLED("全部成交"),
    CANCEL_REQUESTED("撤单请求中"),
    CANCELED("已撤销"),
    CANCEL_REJECTED("撤单被拒");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否为终态（不可再转换）
     */
    public boolean isTerminal() {
        return this == FILLED || this == REJECTED_INVALID || this == REJECTED_WASH || this == CANCELED;
    }
}
