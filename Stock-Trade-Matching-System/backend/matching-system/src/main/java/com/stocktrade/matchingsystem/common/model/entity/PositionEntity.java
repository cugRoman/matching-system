package com.stocktrade.matchingsystem.common.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 持仓持久化实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_position")
public class PositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "share_holder_id", nullable = false, length = 32)
    private String shareHolderId;

    @Column(length = 4, nullable = false)
    private String market;

    @Column(name = "security_id", length = 6, nullable = false)
    private String securityId;

    @Column(name = "total_qty", nullable = false)
    @Builder.Default
    private Integer totalQty = 0;

    @Column(name = "available_qty", nullable = false)
    @Builder.Default
    private Integer availableQty = 0;

    @Column(name = "frozen_qty", nullable = false)
    @Builder.Default
    private Integer frozenQty = 0;

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}