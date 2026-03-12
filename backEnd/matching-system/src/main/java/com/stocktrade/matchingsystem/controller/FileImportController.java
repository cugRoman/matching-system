package com.stocktrade.matchingsystem.controller;

import com.stocktrade.matchingsystem.service.FileImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@CrossOrigin(originPatterns = "*")
public class FileImportController {

    @Autowired
    private FileImportService fileImportService;

    private static final String UPLOAD_DIR = "uploads";

    @PostMapping("/upload/memory")
    public Map<String, Object> uploadToMemory(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Map.of("success", false, "message", "File is empty");
            }

            String fileName = file.getOriginalFilename();
            Path uploadPath = Path.of(UPLOAD_DIR);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String result = fileImportService.importOrdersToMemory(filePath.toString());
            
            return Map.of("success", true, "message", result, "file", fileName);
        } catch (IOException e) {
            return Map.of("success", false, "message", "Error uploading file: " + e.getMessage());
        }
    }

    @PostMapping("/upload/database")
    public Map<String, Object> uploadToDatabase(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Map.of("success", false, "message", "File is empty");
            }

            String fileName = file.getOriginalFilename();
            Path uploadPath = Path.of(UPLOAD_DIR);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String result = fileImportService.importOrdersToDatabase(filePath.toString());
            
            return Map.of("success", true, "message", result, "file", fileName);
        } catch (IOException e) {
            return Map.of("success", false, "message", "Error uploading file: " + e.getMessage());
        }
    }

    @PostMapping("/import/memory")
    public Map<String, Object> importToMemory(@RequestParam String filePath) {
        try {
            String result = fileImportService.importOrdersToMemory(filePath);
            return Map.of("success", true, "message", result);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error importing file: " + e.getMessage());
        }
    }

    @PostMapping("/import/database")
    public Map<String, Object> importToDatabase(@RequestParam String filePath) {
        try {
            String result = fileImportService.importOrdersToDatabase(filePath);
            return Map.of("success", true, "message", result);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error importing file: " + e.getMessage());
        }
    }

    @GetMapping("/sample")
    public Map<String, Object> getSampleFormat() {
        String sample = """
            # 订单导入格式说明
            # 每行一个订单，字段用空格分隔
            # 格式: 订单号 市场 股票代码 方向(B买/S卖) 数量 价格 股东号 [时间戳]
            # 时间戳支持两种格式:
            # 1. 毫秒时间戳 (如: 1704067200000)
            # 2. 年/月/日 时:分:秒 格式 (如: 2024/01/01 08:00:00)
            
            # 示例订单 (使用毫秒时间戳)
            ORD001 XSHG 600519 B 100 205.00 SH001 1704067200000
            ORD002 XSHG 600519 B 150 202.00 SH002 1704067201000
            ORD003 XSHG 600519 S 120 200.00 SH005 1704067204000
            
            # 示例订单 (使用年/月/日 时:分:秒 格式)
            ORD004 XSHG 600519 B 80 198.00 SH004 2024/01/01 08:00:00
            ORD005 XSHG 600519 S 100 203.00 SH006 2024/01/01 08:00:01
            ORD006 XSHG 600519 S 200 205.50 SH007 2024/01/01 08:00:02
            """;
        
        return Map.of(
            "success", true,
            "format", sample,
            "note", "时间戳为可选字段，如果不提供则使用当前时间"
        );
    }
}