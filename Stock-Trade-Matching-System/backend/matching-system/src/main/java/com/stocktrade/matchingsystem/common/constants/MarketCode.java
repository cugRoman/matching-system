package com.stocktrade.matchingsystem.common.constants;

/**
 * 市场代码枚举
 */
public enum MarketCode {
    XSHG("XSHG", "上海证券交易所"),
    XSHE("XSHE", "深圳证券交易所"),
    BJSE("BJSE", "北京证券交易所");

    private final String code;
    private final String description;

    MarketCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValid(String code) {
        for (MarketCode mc : values()) {
            if (mc.code.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static MarketCode fromCode(String code) {
        for (MarketCode mc : values()) {
            if (mc.code.equals(code)) {
                return mc;
            }
        }
        throw new IllegalArgumentException("Invalid market code: " + code);
    }
}
