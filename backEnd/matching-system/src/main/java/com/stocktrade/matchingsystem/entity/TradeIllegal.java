package com.stocktrade.matchingsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "trade_illegals")
public class TradeIllegal {

    @Id
    @Column(name = "cl_order_id", length = 64, nullable = false)
    private String clOrderId;

    @Column(name = "share_holder_id", length = 32, nullable = false)
    private String shareHolderId;

    @Column(name = "security_id", length = 16, nullable = false)
    private String securityId;

    @Column(name = "side", nullable = false, columnDefinition = "char(1)")
    private String side;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "reject_code", nullable = false)
    private Integer rejectCode;

    @Column(name = "reject_time", nullable = false)
    private Long rejectTime;

    @Column(name = "status", nullable = false)
    private Byte status;

    public TradeIllegal() {}

    public TradeIllegal(Order order, Integer rejectCode) {
        this.clOrderId = order.getClOrderId();
        this.shareHolderId = order.getShareHolderId();
        this.securityId = order.getSecurityId();
        this.side = order.getSide();
        this.price = order.getPrice();
        this.qty = order.getQty();
        this.rejectCode = rejectCode;
        this.rejectTime = System.currentTimeMillis();
        this.status = (byte) 4;
    }

    public String getClOrderId() { return clOrderId; }
    public void setClOrderId(String clOrderId) { this.clOrderId = clOrderId; }
    public String getShareHolderId() { return shareHolderId; }
    public void setShareHolderId(String shareHolderId) { this.shareHolderId = shareHolderId; }
    public String getSecurityId() { return securityId; }
    public void setSecurityId(String securityId) { this.securityId = securityId; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Integer getRejectCode() { return rejectCode; }
    public void setRejectCode(Integer rejectCode) { this.rejectCode = rejectCode; }
    public Long getRejectTime() { return rejectTime; }
    public void setRejectTime(Long rejectTime) { this.rejectTime = rejectTime; }
    public Byte getStatus() { return status; }
    public void setStatus(Byte status) { this.status = status; }
}
