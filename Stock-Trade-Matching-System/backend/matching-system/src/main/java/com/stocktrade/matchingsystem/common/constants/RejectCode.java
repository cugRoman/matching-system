package com.stocktrade.matchingsystem.common.constants;

/**
 * 拒绝码枚举
 */
public enum RejectCode {
    // 字段校验类 (1xxx)
    INVALID_FIELD(1001, "INVALID_FIELD"),
    INVALID_MARKET(1002, "INVALID_MARKET"),
    INVALID_SECURITY_ID(1003, "INVALID_SECURITY_ID"),
    INVALID_SIDE(1004, "INVALID_SIDE"),
    INVALID_QTY(1005, "INVALID_QTY"),
    INVALID_PRICE(1006, "INVALID_PRICE"),
    DUPLICATE_ORDER(1007, "DUPLICATE_ORDER_ID"),

    // 风控类 (2xxx)
    WASH_TRADE_DETECTED(2001, "WASH_TRADE_DETECTED"),
    INSUFFICIENT_POSITION(2002, "INSUFFICIENT_POSITION"),

    // 撤单类 (3xxx)
    CANCEL_ORDER_NOT_FOUND(3001, "CANCEL_ORDER_NOT_FOUND"),
    CANCEL_ORDER_FINISHED(3002, "CANCEL_ORDER_FINISHED");

    private final int code;
    private final String text;

    RejectCode(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }
}
