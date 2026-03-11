package com.stocktrade.matchingsystem.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

@Entity
@Table(name = "trade_successes")
public class TradeSuccess {
    private static final AtomicLong idCounter = new AtomicLong(0);
    
    @Id
    @Column(name = "exec_id", length = 64, nullable = false)
    private String execId;

    @Column(name = "security_id", length = 16, nullable = false)
    private String securityId;

    @Column(name = "buy_order_id", length = 64, nullable = false)
    private String buyOrderId;

    @Column(name = "sell_order_id", length = 64, nullable = false)
    private String sellOrderId;

    @Column(name = "buy_share_holder_id", length = 32, nullable = false)
    private String buyShareHolderId;

    @Column(name = "sell_share_holder_id", length = 32, nullable = false)
    private String sellShareHolderId;

    @Column(name = "exec_qty", nullable = false)
    private Integer execQty;

    @Column(name = "exec_price", nullable = false)
    private BigDecimal execPrice;

    @Column(name = "exec_time", nullable = false)
    private Long execTime;

    public TradeSuccess() {}

    public TradeSuccess(String securityId, String buyOrderId, String sellOrderId,
                        String buyShareHolderId, String sellShareHolderId,
                        Integer execQty, Double execPrice) {
        this.execId = "EX" + String.format("%012d", idCounter.incrementAndGet());
        this.securityId = securityId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.buyShareHolderId = buyShareHolderId;
        this.sellShareHolderId = sellShareHolderId;
        this.execQty = execQty;
        this.execPrice = BigDecimal.valueOf(execPrice);
        this.execTime = System.currentTimeMillis();
    }

    public String getExecId() { return execId; }
    public void setExecId(String execId) { this.execId = execId; }
    public String getSecurityId() { return securityId; }
    public void setSecurityId(String securityId) { this.securityId = securityId; }
    public String getBuyOrderId() { return buyOrderId; }
    public void setBuyOrderId(String buyOrderId) { this.buyOrderId = buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public void setSellOrderId(String sellOrderId) { this.sellOrderId = sellOrderId; }
    public String getBuyShareHolderId() { return buyShareHolderId; }
    public void setBuyShareHolderId(String buyShareHolderId) { this.buyShareHolderId = buyShareHolderId; }
    public String getSellShareHolderId() { return sellShareHolderId; }
    public void setSellShareHolderId(String sellShareHolderId) { this.sellShareHolderId = sellShareHolderId; }
    public Integer getExecQty() { return execQty; }
    public void setExecQty(Integer execQty) { this.execQty = execQty; }
    public BigDecimal getExecPrice() { return execPrice; }
    public void setExecPrice(BigDecimal execPrice) { this.execPrice = execPrice; }
    public Long getExecTime() { return execTime; }
    public void setExecTime(Long execTime) { this.execTime = execTime; }
}
