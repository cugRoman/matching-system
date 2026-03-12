package com.stocktrade.matchingsystem.controller;

import com.stocktrade.matchingsystem.dto.OrderRequest;
import com.stocktrade.matchingsystem.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = "*")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/orders")
    public Map<String, Object> addOrder(@RequestBody OrderRequest request) {
        orderService.addOrder(request);
        return Map.of("success", true, "message", "Order added successfully");
    }

    @GetMapping("/orders/all")
    public Map<String, Object> getAllOrders() {
        return orderService.getAllData();
    }

    @GetMapping("/orders/buy-requests")
    public List<Map<String, Object>> getBuyRequests() {
        return orderService.getBuyRequests().stream().map(this::toMap).toList();
    }

    @GetMapping("/orders/sell-requests")
    public List<Map<String, Object>> getSellRequests() {
        return orderService.getSellRequests().stream().map(this::toMap).toList();
    }

    @GetMapping("/orders/exchange-buys")
    public List<Map<String, Object>> getExchangeBuys() {
        return orderService.getExchangeBuys().stream().map(this::toMap).toList();
    }

    @GetMapping("/orders/exchange-sells")
    public List<Map<String, Object>> getExchangeSells() {
        return orderService.getExchangeSells().stream().map(this::toMap).toList();
    }

    @GetMapping("/orders/trade-successes")
    public List<Map<String, Object>> getTradeSuccesses() {
        return orderService.getTradeSuccesses().stream().map(this::toSuccessMap).toList();
    }

    @GetMapping("/orders/trade-illegals")
    public List<Map<String, Object>> getTradeIllegals() {
        return orderService.getTradeIllegals().stream().map(this::toIllegalMap).toList();
    }

    @PostMapping("/orders/match")
    public Map<String, Object> executeMatching() {
        orderService.executeMatching();
        return Map.of("success", true, "message", "Matching executed");
    }

    @PostMapping("/orders/clear")
    public Map<String, Object> clearAll() {
        orderService.clearAll();
        return Map.of("success", true, "message", "All data cleared");
    }

    @GetMapping("/admin/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "orderCount", orderService.getOrderCount());
    }

    @PostMapping("/orders/batch")
    public Map<String, Object> addOrders(@RequestBody List<OrderRequest> requests) {
        orderService.addOrders(requests);
        return Map.of("success", true, "message", "Orders added successfully");
    }

    @PostMapping("/orders/auto-simulate")
    public Map<String, Object> setAutoSimulate(@RequestParam boolean enabled) {
        orderService.setAutoSimulate(enabled);
        return Map.of("success", true, "autoSimulate", enabled);
    }

    @GetMapping("/orders/auto-simulate")
    public Map<String, Object> getAutoSimulate() {
        return Map.of("autoSimulate", orderService.isAutoSimulate());
    }

    private Map<String, Object> toMap(Object obj) {
        try {
            var clazz = obj.getClass();
            return Map.of(
                "clOrderId", getField(obj, clazz, "clOrderId"),
                "market", getField(obj, clazz, "market"),
                "securityId", getField(obj, clazz, "securityId"),
                "side", getField(obj, clazz, "side"),
                "qty", getField(obj, clazz, "qty"),
                "price", getField(obj, clazz, "price"),
                "shareHolderId", getField(obj, clazz, "shareHolderId"),
                "timestamp", getField(obj, clazz, "timestamp"),
                "status", getField(obj, clazz, "status")
            );
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Object> toSuccessMap(Object obj) {
        try {
            var clazz = obj.getClass();
            return Map.of(
                "execId", getField(obj, clazz, "execId"),
                "securityId", getField(obj, clazz, "securityId"),
                "buyOrderId", getField(obj, clazz, "buyOrderId"),
                "sellOrderId", getField(obj, clazz, "sellOrderId"),
                "buyShareHolderId", getField(obj, clazz, "buyShareHolderId"),
                "sellShareHolderId", getField(obj, clazz, "sellShareHolderId"),
                "execQty", getField(obj, clazz, "execQty"),
                "execPrice", getField(obj, clazz, "execPrice"),
                "execTime", getField(obj, clazz, "execTime")
            );
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Object> toIllegalMap(Object obj) {
        try {
            var clazz = obj.getClass();
            return Map.of(
                "clOrderId", getField(obj, clazz, "clOrderId"),
                "shareHolderId", getField(obj, clazz, "shareHolderId"),
                "securityId", getField(obj, clazz, "securityId"),
                "side", getField(obj, clazz, "side"),
                "price", getField(obj, clazz, "price"),
                "qty", getField(obj, clazz, "qty"),
                "rejectCode", getField(obj, clazz, "rejectCode"),
                "rejectTime", getField(obj, clazz, "rejectTime"),
                "status", getField(obj, clazz, "status")
            );
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Object getField(Object obj, Class<?> clazz, String fieldName) {
        try {
            var field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
