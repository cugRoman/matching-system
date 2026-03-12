package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.dto.OrderRequest;
import com.stocktrade.matchingsystem.entity.Order;
import com.stocktrade.matchingsystem.entity.TradeIllegal;
import com.stocktrade.matchingsystem.entity.TradeSuccess;
import com.stocktrade.matchingsystem.repository.OrderRepository;
import com.stocktrade.matchingsystem.repository.TradeIllegalRepository;
import com.stocktrade.matchingsystem.repository.TradeSuccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final Map<String, Order> buyRequests = new ConcurrentHashMap<>();
    private final Map<String, Order> sellRequests = new ConcurrentHashMap<>();
    private final Map<String, Order> exchangeBuys = new ConcurrentHashMap<>();
    private final Map<String, Order> exchangeSells = new ConcurrentHashMap<>();
    private final List<TradeSuccess> tradeSuccesses = Collections.synchronizedList(new ArrayList<>());
    private final List<TradeIllegal> tradeIllegals = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeSuccessRepository tradeSuccessRepository;

    @Autowired
    private TradeIllegalRepository tradeIllegalRepository;

    private boolean autoSimulate = false;
    private int orderCounter = 0;

    @Transactional
    public Order addOrder(OrderRequest request) {
        Order order = createOrderFromRequest(request);
        
        if ("B".equals(order.getSide())) {
            buyRequests.put(order.getClOrderId(), order);
        } else {
            sellRequests.put(order.getClOrderId(), order);
        }

        orderRepository.save(order);

        broadcastUpdate();
        return order;
    }

    @Transactional
    public Order addOrderToMemory(OrderRequest request) {
        if (request.getQty() == null || request.getQty() <= 0) {
            return null;
        }
        
        Order order = createOrderFromRequest(request);
        
        if ("B".equals(order.getSide())) {
            buyRequests.put(order.getClOrderId(), order);
        } else {
            sellRequests.put(order.getClOrderId(), order);
        }

        broadcastUpdate();
        return order;
    }

    private Order createOrderFromRequest(OrderRequest request) {
        return new Order(
            request.getClOrderId(),
            request.getMarket(),
            request.getSecurityId(),
            request.getSide(),
            request.getQty(),
            request.getPrice(),
            request.getShareHolderId(),
            request.getTimestamp()
        );
    }

    @Transactional
    public void addOrders(List<OrderRequest> requests) {
        for (OrderRequest request : requests) {
            addOrder(request);
        }
    }

    @Transactional
    public void addOrdersToMemory(List<OrderRequest> requests) {
        for (OrderRequest request : requests) {
            addOrderToMemory(request);
        }
    }

    public List<Order> getBuyRequests() {
        return buyRequests.values().stream()
            .sorted(Comparator.comparing(Order::getSecurityId)
                .thenComparing(Order::getPrice).reversed()
                .thenComparing(Order::getTimestamp))
            .collect(Collectors.toList());
    }

    public List<Order> getSellRequests() {
        return sellRequests.values().stream()
            .sorted(Comparator.comparing(Order::getSecurityId)
                .thenComparing(Order::getPrice)
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
        return data;
    }

    @Transactional
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
            .forEach(o -> { o.setStatus((byte) 0); allOrders.add(o); });
        sellRequests.values().stream()
            .filter(o -> securityId.equals(o.getSecurityId()))
            .forEach(o -> { o.setStatus((byte) 0); allOrders.add(o); });
        exchangeBuys.values().stream()
            .filter(o -> securityId.equals(o.getSecurityId()))
            .forEach(o -> { o.setStatus((byte) 1); allOrders.add(o); });
        exchangeSells.values().stream()
            .filter(o -> securityId.equals(o.getSecurityId()))
            .forEach(o -> { o.setStatus((byte) 1); allOrders.add(o); });

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
            if (order.getQty() <= 0) continue;
            
            order.setStatus((byte) 4);
            TradeIllegal illegal = new TradeIllegal(order, 1001);
            tradeIllegals.add(illegal);

            // 同步到数据库：更新订单状态 + 写入非法记录
            orderRepository.save(order);
            tradeIllegalRepository.save(illegal);
            buyRequests.remove(order.getClOrderId());
            sellRequests.remove(order.getClOrderId());
            exchangeBuys.remove(order.getClOrderId());
            exchangeSells.remove(order.getClOrderId());
        }

        List<Order> validOrders = allOrders.stream()
            .filter(o -> o.getStatus() != (byte) 4)
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

            // BigDecimal 比较价格：buy.price < sell.price 则无法成交
            if (buy.getPrice().compareTo(sell.getPrice()) < 0) {
                break;
            }

            buy = buyQueue.poll();
            sell = sellQueue.poll();

            int tradeQty = Math.min(buy.getQty(), sell.getQty());

            tradeSuccesses.add(new TradeSuccess(
                securityId,
                buy.getClOrderId(),
                sell.getClOrderId(),
                buy.getShareHolderId(),
                sell.getShareHolderId(),
                tradeQty,
                sell.getPrice().doubleValue()
            ));

            buy.setQty(buy.getQty() - tradeQty);
            sell.setQty(sell.getQty() - tradeQty);

            if (buy.getQty() > 0) {
                buy.setStatus((byte) 2);
                buyQueue.add(buy);
            } else {
                buy.setStatus((byte) 3);
                // 从交易所列表中移除数量为0的订单
                exchangeBuys.remove(buy.getClOrderId());
            }
            if (sell.getQty() > 0) {
                sell.setStatus((byte) 2);
                sellQueue.add(sell);
            } else {
                sell.setStatus((byte) 3);
                // 从交易所列表中移除数量为0的订单
                exchangeSells.remove(sell.getClOrderId());
            }

            // 更新撮合后订单状态到数据库
            orderRepository.save(buy);
            orderRepository.save(sell);
        }

        for (Order o : buyQueue) {
            if (o.getQty() <= 0) {
                o.setStatus((byte) 3);
                // 从交易所列表中移除数量为0的订单
                exchangeBuys.remove(o.getClOrderId());
                orderRepository.save(o);
                continue;
            }
            if (o.getStatus() == (byte) 0) o.setStatus((byte) 1);
            exchangeBuys.put(o.getClOrderId(), o);
            buyRequests.remove(o.getClOrderId());

            orderRepository.save(o);
        }
        for (Order o : sellQueue) {
            if (o.getQty() <= 0) {
                o.setStatus((byte) 3);
                // 从交易所列表中移除数量为0的订单
                exchangeSells.remove(o.getClOrderId());
                orderRepository.save(o);
                continue;
            }
            if (o.getStatus() == (byte) 0) o.setStatus((byte) 1);
            exchangeSells.put(o.getClOrderId(), o);
            sellRequests.remove(o.getClOrderId());

            orderRepository.save(o);
        }

        // 检查该证券在交易所列表中的订单数量
        long exchangeBuyCount = exchangeBuys.values().stream()
            .filter(o -> securityId.equals(o.getSecurityId()))
            .count();
        long exchangeSellCount = exchangeSells.values().stream()
            .filter(o -> securityId.equals(o.getSecurityId()))
            .count();
        
        // 如果该证券在交易所列表中没有订单，则将该证券的所有订单设置为交易完成状态
        if (exchangeBuyCount == 0 && exchangeSellCount == 0) {
            // 从交易所列表中移除该证券的所有订单并设置为交易完成状态
            List<Order> ordersToRemove = new ArrayList<>();
            
            // 处理 exchangeBuys 中的订单
            for (Order order : exchangeBuys.values()) {
                if (securityId.equals(order.getSecurityId())) {
                    order.setStatus((byte) 3);
                    orderRepository.save(order);
                    ordersToRemove.add(order);
                }
            }
            
            // 从 exchangeBuys 中移除订单
            for (Order order : ordersToRemove) {
                exchangeBuys.remove(order.getClOrderId());
            }
            
            ordersToRemove.clear();
            
            // 处理 exchangeSells 中的订单
            for (Order order : exchangeSells.values()) {
                if (securityId.equals(order.getSecurityId())) {
                    order.setStatus((byte) 3);
                    orderRepository.save(order);
                    ordersToRemove.add(order);
                }
            }
            
            // 从 exchangeSells 中移除订单
            for (Order order : ordersToRemove) {
                exchangeSells.remove(order.getClOrderId());
            }
        }
        
        // 从普通订单列表中移除该证券的订单
        buyRequests.entrySet().removeIf(e -> securityId.equals(e.getValue().getSecurityId()));
        sellRequests.entrySet().removeIf(e -> securityId.equals(e.getValue().getSecurityId()));
    }

    @Transactional
    public void clearAll() {
        buyRequests.clear();
        sellRequests.clear();
        exchangeBuys.clear();
        exchangeSells.clear();
        tradeSuccesses.clear();
        tradeIllegals.clear();
        orderCounter = 0;

        tradeSuccessRepository.deleteAll();
        tradeIllegalRepository.deleteAll();
        orderRepository.deleteAll();

        broadcastUpdate();
    }

    @Transactional
    public void clearMemoryOnly() {
        buyRequests.clear();
        sellRequests.clear();
        exchangeBuys.clear();
        exchangeSells.clear();
        tradeSuccesses.clear();
        tradeIllegals.clear();
        orderCounter = 0;

        broadcastUpdate();
    }

    @Transactional
    public void clearDatabaseOnly() {
        tradeSuccessRepository.deleteAll();
        tradeIllegalRepository.deleteAll();
        orderRepository.deleteAll();
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

    @Scheduled(fixedRate = 5000)
    public void autoSimulate() {
        if (!autoSimulate) return;
        
        generateRandomOrder();
        executeMatching();
    }

    private synchronized void generateRandomOrder() {
        String[] securities = {"600519", "000001", "601318"};
        String[] holders = {"SH001", "SH002", "SH003", "SH004", "SH005", "SH006", "SH007"};
        
        String securityId = securities[new Random().nextInt(securities.length)];
        String holder = holders[new Random().nextInt(holders.length)];
        String side = new Random().nextBoolean() ? "B" : "S";
        double basePrice = 100.0 + new Random().nextDouble() * 100;
        int qty = (new Random().nextInt(10) + 1) * 10;
        
        orderCounter++;
        String orderId = String.format("ORD%05d", orderCounter);
        
        OrderRequest request = new OrderRequest();
        request.setClOrderId(orderId);
        request.setMarket("XSHG");
        request.setSecurityId(securityId);
        request.setSide(side);
        request.setQty(qty);
        request.setPrice(basePrice);
        request.setShareHolderId(holder);
        
        addOrder(request);
    }

    @Transactional
    public Map<String, Object> saveMemoryToDatabase() {
        int orderCount = 0;
        int successCount = 0;
        int illegalCount = 0;
        
        for (Order order : buyRequests.values()) {
            orderRepository.save(order);
            orderCount++;
        }
        for (Order order : sellRequests.values()) {
            orderRepository.save(order);
            orderCount++;
        }
        for (Order order : exchangeBuys.values()) {
            orderRepository.save(order);
            orderCount++;
        }
        for (Order order : exchangeSells.values()) {
            orderRepository.save(order);
            orderCount++;
        }
        
        for (TradeSuccess trade : tradeSuccesses) {
            tradeSuccessRepository.save(trade);
            successCount++;
        }
        for (TradeIllegal trade : tradeIllegals) {
            tradeIllegalRepository.save(trade);
            illegalCount++;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("orders", orderCount);
        result.put("successes", successCount);
        result.put("illegals", illegalCount);
        return result;
    }

    private void broadcastUpdate() {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/orders", getAllData());
        }
    }
}
