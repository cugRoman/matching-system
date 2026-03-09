package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.dto.OrderRequest;
import com.stocktrade.matchingsystem.entity.Order;
import com.stocktrade.matchingsystem.entity.TradeIllegal;
import com.stocktrade.matchingsystem.entity.TradeSuccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SimulateService {

    private final Map<String, Order> buyRequests = new ConcurrentHashMap<>();
    private final Map<String, Order> sellRequests = new ConcurrentHashMap<>();
    private final Map<String, Order> exchangeBuys = new ConcurrentHashMap<>();
    private final Map<String, Order> exchangeSells = new ConcurrentHashMap<>();
    private final List<TradeSuccess> tradeSuccesses = Collections.synchronizedList(new ArrayList<>());
    private final List<TradeIllegal> tradeIllegals = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private boolean autoSimulate = false;
    private int orderCounter = 0;
    private String currentSecurityId = "600519";
    private double meanPrice = 100.0;
    private final Random random = new Random();

    public Order addOrder(OrderRequest request) {
        Order order = new Order(
            request.getClOrderId(),
            request.getMarket(),
            request.getSecurityId(),
            request.getSide(),
            request.getQty(),
            request.getPrice(),
            request.getShareHolderId(),
            request.getAccountId()
        );

        if ("B".equals(order.getSide())) {
            buyRequests.put(order.getClOrderId(), order);
        } else {
            sellRequests.put(order.getClOrderId(), order);
        }
        broadcastUpdate();
        return order;
    }

    public List<Order> getBuyRequests() {
        return buyRequests.values().stream()
            .sorted(Comparator.comparing(Order::getPrice).reversed()
                .thenComparing(Order::getTimestamp))
            .collect(Collectors.toList());
    }

    public List<Order> getSellRequests() {
        return sellRequests.values().stream()
            .sorted(Comparator.comparing(Order::getPrice)
                .thenComparing(Order::getTimestamp))
            .collect(Collectors.toList());
    }

    public List<Order> getExchangeBuys() {
        return exchangeBuys.values().stream()
            .sorted(Comparator.comparing(Order::getPrice).reversed()
                .thenComparing(Order::getTimestamp))
            .collect(Collectors.toList());
    }

    public List<Order> getExchangeSells() {
        return exchangeSells.values().stream()
            .sorted(Comparator.comparing(Order::getPrice)
                .thenComparing(Order::getTimestamp))
            .collect(Collectors.toList());
    }

    public List<TradeSuccess> getTradeSuccesses() {
        return tradeSuccesses.stream()
            .sorted(Comparator.comparing(TradeSuccess::getExecTime).reversed())
            .collect(Collectors.toList());
    }

    public List<TradeIllegal> getTradeIllegals() {
        return tradeIllegals.stream()
            .sorted(Comparator.comparing(TradeIllegal::getRejectTime).reversed())
            .collect(Collectors.toList());
    }

    public Map<String, Object> getAllData() {
        Map<String, Object> data = new HashMap<>();
        data.put("buyRequests", getBuyRequests());
        data.put("sellRequests", getSellRequests());
        data.put("exchangeBuys", getExchangeBuys());
        data.put("exchangeSells", getExchangeSells());
        data.put("tradeSuccesses", getTradeSuccesses());
        data.put("tradeIllegals", getTradeIllegals());
        data.put("meanPrice", meanPrice);
        data.put("currentSecurityId", currentSecurityId);
        return data;
    }

    public void executeMatching() {
        Set<String> allSecurities = new HashSet<>();
        allSecurities.addAll(buyRequests.values().stream()
            .map(Order::getSecurityId).collect(Collectors.toSet()));
        allSecurities.addAll(sellRequests.values().stream()
            .map(Order::getSecurityId).collect(Collectors.toSet()));
        allSecurities.addAll(exchangeBuys.values().stream()
            .map(Order::getSecurityId).collect(Collectors.toSet()));
        allSecurities.addAll(exchangeSells.values().stream()
            .map(Order::getSecurityId).collect(Collectors.toSet()));

        for (String securityId : allSecurities) {
            matchForSecurity(securityId);
        }
        broadcastUpdate();
    }

    private void matchForSecurity(String securityId) {
        List<Order> allOrders = new ArrayList<>();
        
        buyRequests.values().stream()
            .filter(o -> securityId.equals(o.getSecurityId()))
            .forEach(o -> { o.setStatus(0); allOrders.add(o); });
        sellRequests.values().stream()
            .filter(o -> securityId.equals(o.getSecurityId()))
            .forEach(o -> { o.setStatus(0); allOrders.add(o); });
        exchangeBuys.values().stream()
            .filter(o -> securityId.equals(o.getSecurityId()))
            .forEach(o -> { o.setStatus(1); allOrders.add(o); });
        exchangeSells.values().stream()
            .filter(o -> securityId.equals(o.getSecurityId()))
            .forEach(o -> { o.setStatus(1); allOrders.add(o); });

        Map<String, List<Order>> byHolder = allOrders.stream()
            .collect(Collectors.groupingBy(Order::getShareHolderId));

        List<Order> illegalOrders = new ArrayList<>();
        for (Map.Entry<String, List<Order>> entry : byHolder.entrySet()) {
            boolean hasBuy = entry.getValue().stream().anyMatch(o -> "B".equals(o.getSide()));
            boolean hasSell = entry.getValue().stream().anyMatch(o -> "S".equals(o.getSide()));
            if (hasBuy && hasSell) {
                illegalOrders.addAll(entry.getValue());
            }
        }

        for (Order order : illegalOrders) {
            order.setStatus(4);
            tradeIllegals.add(new TradeIllegal(order, 1001));
            buyRequests.remove(order.getClOrderId());
            sellRequests.remove(order.getClOrderId());
            exchangeBuys.remove(order.getClOrderId());
            exchangeSells.remove(order.getClOrderId());
        }

        List<Order> validOrders = allOrders.stream()
            .filter(o -> o.getStatus() != 4)
            .collect(Collectors.toList());

        PriorityQueue<Order> buyQueue = new PriorityQueue<>(
            Comparator.comparing(Order::getPrice).reversed()
                .thenComparing(Order::getTimestamp)
        );
        PriorityQueue<Order> sellQueue = new PriorityQueue<>(
            Comparator.comparing(Order::getPrice)
                .thenComparing(Order::getTimestamp)
        );

        for (Order o : validOrders) {
            if (o.getQty() <= 0) continue;
            if ("B".equals(o.getSide())) {
                buyQueue.add(o);
            } else {
                sellQueue.add(o);
            }
        }

        while (!buyQueue.isEmpty() && !sellQueue.isEmpty()) {
            Order buy = buyQueue.peek();
            Order sell = sellQueue.peek();

            if (buy.getPrice() < sell.getPrice()) {
                break;
            }

            buy = buyQueue.poll();
            sell = sellQueue.poll();

            int tradeQty = Math.min(buy.getQty(), sell.getQty());
            
            if (tradeQty > 0) {
                double execPrice = sell.getPrice();
                meanPrice = execPrice;

                tradeSuccesses.add(new TradeSuccess(
                    securityId,
                    buy.getClOrderId(),
                    sell.getClOrderId(),
                    buy.getShareHolderId(),
                    sell.getShareHolderId(),
                    tradeQty,
                    execPrice
                ));
            }

            buy.setQty(buy.getQty() - tradeQty);
            sell.setQty(sell.getQty() - tradeQty);

            if (buy.getQty() > 0) {
                buy.setStatus(2);
                buyQueue.add(buy);
            }
            if (sell.getQty() > 0) {
                sell.setStatus(2);
                sellQueue.add(sell);
            }
        }

        for (Order o : buyQueue) {
            if (o.getStatus() == 0) o.setStatus(1);
            exchangeBuys.put(o.getClOrderId(), o);
            buyRequests.remove(o.getClOrderId());
        }
        for (Order o : sellQueue) {
            if (o.getStatus() == 0) o.setStatus(1);
            exchangeSells.put(o.getClOrderId(), o);
            sellRequests.remove(o.getClOrderId());
        }

        buyRequests.entrySet().removeIf(e -> securityId.equals(e.getValue().getSecurityId()));
        sellRequests.entrySet().removeIf(e -> securityId.equals(e.getValue().getSecurityId()));
    }

    public void clearAll() {
        buyRequests.clear();
        sellRequests.clear();
        exchangeBuys.clear();
        exchangeSells.clear();
        tradeSuccesses.clear();
        tradeIllegals.clear();
        orderCounter = 0;
        meanPrice = 100.0;
        broadcastUpdate();
    }

    public int getOrderCount() {
        return buyRequests.size() + sellRequests.size() + 
               exchangeBuys.size() + exchangeSells.size();
    }

    public void setAutoSimulate(boolean auto) {
        this.autoSimulate = auto;
    }

    public boolean isAutoSimulate() {
        return autoSimulate;
    }

    public void setCurrentSecurityId(String securityId) {
        this.currentSecurityId = securityId;
    }

    public String getCurrentSecurityId() {
        return currentSecurityId;
    }

    public double getMeanPrice() {
        return meanPrice;
    }

    @Scheduled(fixedRate = 5000)
    public void autoSimulate() {
        if (!autoSimulate) return;
        
        for (int i = 0; i < 10; i++) {
            generateOrder();
        }
        executeMatching();
    }

    private synchronized void generateOrder() {
        int holderNum = random.nextInt(99) + 1;
        String holder = String.format("%02d", holderNum);
        
        String side = random.nextBoolean() ? "B" : "S";
        
        double price = meanPrice + random.nextGaussian() * 2;
        price = Math.max(price, 1.0);
        
        int qty = (random.nextInt(9) + 1) * 100;
        
        orderCounter++;
        String orderId = String.format("SIM%06d", orderCounter);
        
        OrderRequest request = new OrderRequest();
        request.setClOrderId(orderId);
        request.setMarket("XSHG");
        request.setSecurityId(currentSecurityId);
        request.setSide(side);
        request.setQty(qty);
        request.setPrice(price);
        request.setShareHolderId(holder);
        request.setAccountId("ACC" + holder);
        
        addOrder(request);
    }

    private void broadcastUpdate() {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/simulate", getAllData());
        }
    }
}
