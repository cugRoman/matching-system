package com.stocktrade.matchingsystem.entity;

public class TradeIllegal {
    private String clOrderId;
    private String shareHolderId;
    private String securityId;
    private String side;
    private Double price;
    private Integer qty;
    private Integer rejectCode;
    private Long rejectTime;
    private Integer status;

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
        this.status = 4;
    }

    public String getClOrderId() { return clOrderId; }
    public void setClOrderId(String clOrderId) { this.clOrderId = clOrderId; }
    public String getShareHolderId() { return shareHolderId; }
    public void setShareHolderId(String shareHolderId) { this.shareHolderId = shareHolderId; }
    public String getSecurityId() { return securityId; }
    public void setSecurityId(String securityId) { this.securityId = securityId; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Integer getRejectCode() { return rejectCode; }
    public void setRejectCode(Integer rejectCode) { this.rejectCode = rejectCode; }
    public Long getRejectTime() { return rejectTime; }
    public void setRejectTime(Long rejectTime) { this.rejectTime = rejectTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
