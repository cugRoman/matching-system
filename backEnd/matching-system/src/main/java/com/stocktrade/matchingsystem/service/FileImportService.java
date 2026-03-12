package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.dto.OrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@Service
public class FileImportService {

    @Autowired
    private OrderService orderService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    }

    public List<OrderRequest> importOrdersFromFile(String filePath) throws IOException, ParseException {
        List<OrderRequest> orders = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split("\\s+");
                
                if (parts.length < 7) {
                    System.err.println("Line " + lineNumber + ": Invalid format, skipping: " + line);
                    continue;
                }
                
                OrderRequest request = new OrderRequest();
                request.setClOrderId(parts[0]);
                request.setMarket(parts[1]);
                request.setSecurityId(parts[2]);
                request.setSide(parts[3]);
                
                try {
                    request.setQty(Integer.parseInt(parts[4]));
                    request.setPrice(Double.parseDouble(parts[5]));
                } catch (NumberFormatException e) {
                    System.err.println("Line " + lineNumber + ": Invalid number format, skipping: " + line);
                    continue;
                }
                
                request.setShareHolderId(parts[6]);
                
                if (parts.length >= 8) {
                    String timestampStr = parts[7];
                    try {
                        if (timestampStr.matches("\\d+")) {
                            request.setTimestamp(Long.parseLong(timestampStr));
                        } else {
                            Date date = DATE_FORMAT.parse(timestampStr);
                            request.setTimestamp(date.getTime());
                        }
                    } catch (Exception e) {
                        System.err.println("Line " + lineNumber + ": Invalid timestamp format, using current time: " + timestampStr);
                        request.setTimestamp(System.currentTimeMillis());
                    }
                } else {
                    request.setTimestamp(System.currentTimeMillis());
                }
                
                orders.add(request);
            }
        }
        
        return orders;
    }

    public String importOrdersToMemory(String filePath) {
        try {
            List<OrderRequest> orders = importOrdersFromFile(filePath);
            orderService.addOrdersToMemory(orders);
            return "Successfully imported " + orders.size() + " orders from " + filePath;
        } catch (IOException | ParseException e) {
            return "Error importing orders: " + e.getMessage();
        }
    }

    public String importOrdersToDatabase(String filePath) {
        try {
            List<OrderRequest> orders = importOrdersFromFile(filePath);
            orderService.addOrders(orders);
            return "Successfully imported " + orders.size() + " orders to database from " + filePath;
        } catch (IOException | ParseException e) {
            return "Error importing orders: " + e.getMessage();
        }
    }
}