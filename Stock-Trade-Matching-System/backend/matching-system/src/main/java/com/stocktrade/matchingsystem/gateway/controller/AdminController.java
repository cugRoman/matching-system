package com.stocktrade.matchingsystem.gateway.controller;

import com.stocktrade.matchingsystem.common.model.entity.ExecutionEntity;
import com.stocktrade.matchingsystem.common.model.entity.ReportEntity;
import com.stocktrade.matchingsystem.persistence.repository.ExecutionRepository;
import com.stocktrade.matchingsystem.persistence.repository.ReportRepository;
import com.stocktrade.matchingsystem.risk.OrderIndex;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理/监控接口 — 供企业端监控系统状态
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "系统管理与监控")
public class AdminController {

    private final OrderIndex orderIndex;
    private final ExecutionRepository executionRepository;
    private final ReportRepository reportRepository;

    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString()));
    }

    @GetMapping("/stats")
    @Operation(summary = "获取系统统计信息")
    public ResponseEntity<Map<String, Object>> getStats() {
        // TODO: 扩展更丰富的统计维度
        return ResponseEntity.ok(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "message", "Statistics endpoint - to be expanded"));
    }

    @GetMapping("/executions")
    @Operation(summary = "Get all executions")
    public ResponseEntity<List<ExecutionEntity>> getExecutions() {
        return ResponseEntity.ok(executionRepository.findAll());
    }

    @GetMapping("/reports")
    @Operation(summary = "Get all reports")
    public ResponseEntity<List<ReportEntity>> getReports() {
        return ResponseEntity.ok(reportRepository.findAll());
    }
}
