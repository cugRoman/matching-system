package com.stocktrade.matchingsystem.controller;

import com.stocktrade.matchingsystem.service.SimulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulate")
@CrossOrigin(originPatterns = "*")
public class SimulateController {

    @Autowired
    private SimulateService simulateService;

    @GetMapping("/all")
    public Map<String, Object> getAllOrders() {
        return simulateService.getAllData();
    }

    @PostMapping("/match")
    public Map<String, Object> executeMatching() {
        simulateService.executeMatching();
        return Map.of("success", true, "message", "Matching executed");
    }

    @PostMapping("/clear")
    public Map<String, Object> clearAll() {
        simulateService.clearAll();
        return Map.of("success", true, "message", "All data cleared");
    }

    @PostMapping("/auto-simulate")
    public Map<String, Object> setAutoSimulate(@RequestParam boolean enabled) {
        simulateService.setAutoSimulate(enabled);
        return Map.of("success", true, "autoSimulate", enabled);
    }

    @GetMapping("/auto-simulate")
    public Map<String, Object> getAutoSimulate() {
        return Map.of("autoSimulate", simulateService.isAutoSimulate());
    }

    @PostMapping("/security/{securityId}")
    public Map<String, Object> setSecurityId(@PathVariable String securityId) {
        simulateService.setCurrentSecurityId(securityId);
        return Map.of("success", true, "securityId", securityId);
    }

    @GetMapping("/security")
    public Map<String, Object> getSecurityId() {
        return Map.of(
            "securityId", simulateService.getCurrentSecurityId(),
            "meanPrice", simulateService.getMeanPrice()
        );
    }
}
