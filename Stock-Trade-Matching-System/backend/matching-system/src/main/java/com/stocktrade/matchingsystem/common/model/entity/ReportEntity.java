package com.stocktrade.matchingsystem.common.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 报告持久化实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_report")
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", length = 32)
    private String reportId;

    @Column(name = "type", length = 16)
    private String type;

    @Column(name = "ref_cl_order_id", length = 16)
    private String refClOrdId;

    @Column(name = "ts")
    @Builder.Default
    private Instant ts = Instant.now();

    @Column(name = "market", length = 4)
    private String market;

    @Column(name = "security_id", length = 6)
    private String securityId;

    @Column(name = "side", length = 1)
    private String side;

    @Column(name = "qty")
    private Integer qty;

    @Column(name = "price")
    private Double price;

    @Column(name = "share_holder_id", length = 10)
    private String shareHolderId;

    @Column(name = "reject_code")
    private Integer rejectCode;

    @Column(name = "reject_text", length = 64)
    private String rejectText;

    @Column(name = "exec_id", length = 16)
    private String execId;

    @Column(name = "exec_qty")
    private Integer execQty;

    @Column(name = "exec_price")
    private Double execPrice;

    @Column(name = "exec_source", length = 16)
    private String execSource;

    @Column(name = "orig_cl_order_id", length = 16)
    private String origClOrdId;

    @Column(name = "cancel_text", length = 64)
    private String cancelText;

    @Column(name = "canceled_qty")
    private Integer canceledQty;
}
