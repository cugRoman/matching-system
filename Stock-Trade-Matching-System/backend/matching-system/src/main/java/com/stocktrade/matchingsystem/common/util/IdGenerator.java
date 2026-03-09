package com.stocktrade.matchingsystem.common.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID 生成器 — 生成全局唯一的 execId、内部撤单ID等
 */
public class IdGenerator {

    private static final AtomicLong EXEC_COUNTER = new AtomicLong(0);
    private static final AtomicLong CANCEL_COUNTER = new AtomicLong(0);
    private static final AtomicLong REPORT_COUNTER = new AtomicLong(0);
    private static final AtomicLong ORDER_COUNTER = new AtomicLong(0);
    private static final AtomicLong SHAREHOLDER_COUNTER = new AtomicLong(100000);

    /**
     * 生成成交编号 EXEC_000000001
     */
    public static String nextExecId() {
        return String.format("EXEC_%09d", EXEC_COUNTER.incrementAndGet());
    }

    /**
     * 生成系统内部撤单编号 SYS_CXL_000001
     */
    public static String nextSystemCancelId() {
        return String.format("SYS_CXL_%06d", CANCEL_COUNTER.incrementAndGet());
    }

    /**
     * 生成报告编号 RPT_000000001
     */
    public static String nextReportId() {
        return String.format("RPT_%09d", REPORT_COUNTER.incrementAndGet());
    }

    /**
     * 鐢熸垚瀹㈡埛濮旀墭缂栧彿 ORD_000000001
     */
    public static String nextClOrderId() {
        return String.format("ORD_%09d", ORDER_COUNTER.incrementAndGet());
    }

    /**
     * Generate shareholder id SH_100001
     */
    public static String nextShareHolderId() {
        return String.format("SH_%06d", SHAREHOLDER_COUNTER.incrementAndGet());
    }

    /**
     * 重置计数器 (仅用于测试)
     */
    public static void reset() {
        EXEC_COUNTER.set(0);
        CANCEL_COUNTER.set(0);
        REPORT_COUNTER.set(0);
        ORDER_COUNTER.set(0);
        SHAREHOLDER_COUNTER.set(100000);
    }
}
