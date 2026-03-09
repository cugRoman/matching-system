package com.stocktrade.matchingsystem.common.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 订单持久化实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_order")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cl_order_id", unique = true, nullable = false, length = 16)
    private String clOrderId;

    @Column(length = 4)
    private String market;

    @Column(name = "security_id", length = 6)
    private String securityId;

    @Column(length = 1)
    private String side;

    private Integer qty;

    private Double price;

    @Column(name = "share_holder_id", length = 10)
    private String shareHolderId;

    @Column(name = "trade_date")
    private LocalDate tradeDate;

    @Column(length = 20)
    private String status;

    @Column(name = "sent_to_exchange")
    @Builder.Default
    private Boolean sentToExchange = false;

    @Column(name = "qty_filled")
    @Builder.Default
    private Integer qtyFilled = 0;

    @Column(name = "qty_remaining")
    private Integer qtyRemaining;

    @Column(name = "is_wash_trade")
    @Builder.Default
    private Boolean washTrade = false;

    @Column(name = "recv_time")
    @Builder.Default
    private Instant recvTime = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
