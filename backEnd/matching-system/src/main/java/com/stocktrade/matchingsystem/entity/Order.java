package com.stocktrade.matchingsystem.entity;

public class Order {
    private String clOrderId;
    private String market;
    private String securityId;
    private String side;
    private Integer qty;
    private Double price;
    private String shareHolderId;
    private String accountId;
    private Long timestamp;
    private Integer status;

    public Order() {}

    public Order(String clOrderId, String market, String securityId, String side, 
                 Integer qty, Double price, String shareHolderId, String accountId) {
        this.clOrderId = clOrderId;
        this.market = market;
        this.securityId = securityId;
        this.side = side;
        this.qty = qty;
        this.price = price;
        this.shareHolderId = shareHolderId;
        this.accountId = accountId;
        this.timestamp = System.currentTimeMillis();
        this.status = 0;
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
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getShareHolderId() { return shareHolderId; }
    public void setShareHolderId(String shareHolderId) { this.shareHolderId = shareHolderId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
