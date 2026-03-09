package com.stocktrade.matchingsystem.common.constants;

/**
 * 买卖方向枚举
 * B = Buy = 买入, S = Sell = 卖出
 */
public enum OrderSide {
    B("B", "买入"),
    S("S", "卖出");

    private final String code;
    private final String description;

    OrderSide(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 获取反向方向
     */
    public OrderSide opposite() {
        return this == B ? S : B;
    }

    public static boolean isValid(String code) {
        return "B".equals(code) || "S".equals(code);
    }

    public static OrderSide fromCode(String code) {
        for (OrderSide side : values()) {
            if (side.code.equals(code)) {
                return side;
            }
        }
        throw new IllegalArgumentException("Invalid side: " + code);
    }
}
