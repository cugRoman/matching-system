package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.common.model.dto.*;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 订单处理结果 — 封装处理管线的全部输出
 */
@Getter
public class OrderProcessResult {

    public enum Type {
        /** 校验拒绝 */
        REJECTED_INVALID,
        /** 对敲拦截 */
        REJECTED_WASH,
        /** 已受理 */
        ACCEPTED
    }

    private final Type type;
    private final RejectReportDTO rejectReport;
    private final ConfirmReportDTO confirmReport;
    private final List<ExecReportDTO> execReports;
    private final List<CancelConfirmDTO> cancelConfirms;

    private OrderProcessResult(Type type,
            RejectReportDTO rejectReport,
            ConfirmReportDTO confirmReport,
            List<ExecReportDTO> execReports,
            List<CancelConfirmDTO> cancelConfirms) {
        this.type = type;
        this.rejectReport = rejectReport;
        this.confirmReport = confirmReport;
        this.execReports = execReports != null ? execReports : Collections.emptyList();
        this.cancelConfirms = cancelConfirms != null ? cancelConfirms : Collections.emptyList();
    }

    public static OrderProcessResult rejected(RejectReportDTO reject) {
        return new OrderProcessResult(Type.REJECTED_INVALID, reject, null, null, null);
    }

    public static OrderProcessResult accepted(ConfirmReportDTO confirm,
            List<ExecReportDTO> execReports,
            List<CancelConfirmDTO> cancelConfirms) {
        return new OrderProcessResult(Type.ACCEPTED, null, confirm, execReports, cancelConfirms);
    }

    public static OrderProcessResult washRejected(ConfirmReportDTO confirm,
            RejectReportDTO washReject,
            List<CancelConfirmDTO> cancelConfirms) {
        return new OrderProcessResult(Type.REJECTED_WASH, washReject, confirm, null, cancelConfirms);
    }
}
