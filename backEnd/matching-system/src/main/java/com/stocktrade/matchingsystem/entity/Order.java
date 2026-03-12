package com.stocktrade.matchingsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "cl_order_id", length = 64, nullable = false)
    private String clOrderId;

    @Transient
    private String market;

    @Column(name = "security_id", length = 16, nullable = false)
    private String securityId;

    @Column(name = "side", nullable = false, columnDefinition = "char(1)")
    private String side;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "share_holder_id", length = 32, nullable = false)
    private String shareHolderId;

    @Transient
    private String accountId;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "status", nullable = false)
    private Byte status;

    public Order() {}

    public Order(String clOrderId, String market, String securityId, String side,
                 Integer qty, Double price, String shareHolderId, String accountId) {
        this.clOrderId = clOrderId;
        this.market = market;
        this.securityId = securityId;
        this.side = side;
        this.qty = qty;
        this.price = BigDecimal.valueOf(price);
        this.shareHolderId = shareHolderId;
        this.accountId = accountId;
        this.timestamp = System.currentTimeMillis();
        this.status = (byte) 0;
    }

    public String getClOrderId() { return clOrderId; }
    public void setClOrderId(String clOrderId) { this.clOrderId = clOrderId; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public String getSecurityId() { return securityId; }
    public void setSecurityId(String securityId) { this.securityId = securityId; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getShareHolderId() { return shareHolderId; }
    public void setShareHolderId(String shareHolderId) { this.shareHolderId = shareHolderId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public Byte getStatus() { return status; }
    public void setStatus(Byte status) { this.status = status; }
}
