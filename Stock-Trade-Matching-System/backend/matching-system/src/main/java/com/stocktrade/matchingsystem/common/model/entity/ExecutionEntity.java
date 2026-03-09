package com.stocktrade.matchingsystem.common.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 成交记录持久化实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_execution")
public class ExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exec_id", nullable = false, length = 16)
    private String execId;

    @Column(name = "cl_order_id", length = 16)
    private String clOrderId;

    @Column(length = 4)
    private String market;

    @Column(name = "security_id", length = 6)
    private String securityId;

    @Column(length = 1)
    private String side;

    @Column(name = "exec_qty")
    private Integer execQty;

    @Column(name = "exec_price")
    private Double execPrice;

    @Column(name = "share_holder_id", length = 10)
    private String shareHolderId;

    /** 成交类型: EXCHANGE(交易所) / INTERNAL(内部撮合) */
    @Column(name = "exec_type", length = 10)
    private String execType;

    /** 对手方订单编号 (内部撮合时) */
    @Column(name = "counter_order_id", length = 16)
    private String counterOrderId;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
