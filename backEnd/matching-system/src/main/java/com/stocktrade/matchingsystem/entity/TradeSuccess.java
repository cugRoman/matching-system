package com.stocktrade.matchingsystem.entity;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class TradeSuccess {
    private static final AtomicLong idCounter = new AtomicLong(0);
    
    private String execId;
    private String securityId;
    private String buyOrderId;
    private String sellOrderId;
    private String buyShareHolderId;
    private String sellShareHolderId;
    private Integer execQty;
    private Double execPrice;
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
        this.execPrice = execPrice;
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
    public Double getExecPrice() { return execPrice; }
    public void setExecPrice(Double execPrice) { this.execPrice = execPrice; }
    public Long getExecTime() { return execTime; }
    public void setExecTime(Long execTime) { this.execTime = execTime; }
}
