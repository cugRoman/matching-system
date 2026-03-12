package com.stocktrade.matchingsystem.dto;

public class OrderRequest {
    private String clOrderId;
    private String market;
    private String securityId;
    private String side;
    private Integer qty;
    private Double price;
    private String shareHolderId;
    private Long timestamp;

    public OrderRequest() {}

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
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getShareHolderId() { return shareHolderId; }
    public void setShareHolderId(String shareHolderId) { this.shareHolderId = shareHolderId; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
