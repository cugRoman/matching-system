package com.stocktrade.matchingsystem.risk;

import com.stocktrade.matchingsystem.common.constants.OrderSide;
import com.stocktrade.matchingsystem.common.model.InternalOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存订单索引：
 * 用于对敲检测与内部撮合检索。
 */
@Slf4j
@Component
public class OrderIndex {

    /**
     * 对敲索引。
     * key = tradeDate|shareHolderId|market|securityId
     * value = { BUY -> queue, SELL -> queue }
     */
    private final ConcurrentHashMap<String, Map<OrderSide, Deque<InternalOrder>>> washIndex = new ConcurrentHashMap<>();

    /**
     * 内部撮合索引。
     * key = market|securityId
     * value = { BUY -> price map, SELL -> price map }
     */
    private final ConcurrentHashMap<String, Map<OrderSide, TreeMap<Double, Deque<InternalOrder>>>> matchIndex = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Object> washLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> matchLocks = new ConcurrentHashMap<>();

    public Object getWashLock(String washKey) {
        return washLocks.computeIfAbsent(washKey, k -> new Object());
    }

    public Object getMatchLock(String matchKey) {
        return matchLocks.computeIfAbsent(matchKey, k -> new Object());
    }

    public List<InternalOrder> findWashCounterOrders(InternalOrder incoming) {
        Map<OrderSide, Deque<InternalOrder>> sideMap = washIndex.get(incoming.washTradeKey());
        if (sideMap == null) {
            return Collections.emptyList();
        }
        Deque<InternalOrder> counters = sideMap.get(incoming.getSide().opposite());
        if (counters == null || counters.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(counters);
    }

    public List<InternalOrder> findMatchCounterOrders(InternalOrder incoming) {
        Map<OrderSide, TreeMap<Double, Deque<InternalOrder>>> sideMap = matchIndex.get(incoming.matchKey());
        if (sideMap == null) {
            return Collections.emptyList();
        }

        OrderSide counterSide = incoming.getSide().opposite();
        TreeMap<Double, Deque<InternalOrder>> counterBook = sideMap.get(counterSide);
        if (counterBook == null || counterBook.isEmpty()) {
            return Collections.emptyList();
        }

        List<InternalOrder> result = new ArrayList<>();
        if (incoming.getSide() == OrderSide.B) {
            NavigableMap<Double, Deque<InternalOrder>> eligible = counterBook.headMap(incoming.getPrice(), true);
            for (Deque<InternalOrder> queue : eligible.values()) {
                result.addAll(queue);
            }
        } else {
            NavigableMap<Double, Deque<InternalOrder>> eligible = counterBook.tailMap(incoming.getPrice(), true);
            for (Deque<InternalOrder> queue : eligible.descendingMap().values()) {
                result.addAll(queue);
            }
        }
        return result;
    }

    public void addOrder(InternalOrder order) {
        String washKey = order.washTradeKey();
        washIndex.computeIfAbsent(washKey, k -> {
            Map<OrderSide, Deque<InternalOrder>> m = new EnumMap<>(OrderSide.class);
            m.put(OrderSide.B, new ArrayDeque<>());
            m.put(OrderSide.S, new ArrayDeque<>());
            return m;
        });
        washIndex.get(washKey).get(order.getSide()).addLast(order);

        String matchKey = order.matchKey();
        matchIndex.computeIfAbsent(matchKey, k -> {
            Map<OrderSide, TreeMap<Double, Deque<InternalOrder>>> m = new EnumMap<>(OrderSide.class);
            m.put(OrderSide.B, new TreeMap<>());
            m.put(OrderSide.S, new TreeMap<>());
            return m;
        });
        TreeMap<Double, Deque<InternalOrder>> book = matchIndex.get(matchKey).get(order.getSide());
        book.computeIfAbsent(order.getPrice(), p -> new ArrayDeque<>()).addLast(order);

        log.debug("Order indexed: {} {} {} @ {} [{}]",
                order.getClOrderId(), order.getSide(), order.getSecurityId(),
                order.getPrice(), matchKey);
    }

    public void removeOrder(InternalOrder order) {
        String washKey = order.washTradeKey();
        Map<OrderSide, Deque<InternalOrder>> washSideMap = washIndex.get(washKey);
        if (washSideMap != null) {
            Deque<InternalOrder> queue = washSideMap.get(order.getSide());
            if (queue != null) {
                queue.removeIf(o -> o.getClOrderId().equals(order.getClOrderId()));
            }
        }

        String matchKey = order.matchKey();
        Map<OrderSide, TreeMap<Double, Deque<InternalOrder>>> matchSideMap = matchIndex.get(matchKey);
        if (matchSideMap == null) {
            return;
        }
        TreeMap<Double, Deque<InternalOrder>> book = matchSideMap.get(order.getSide());
        if (book == null) {
            return;
        }
        Deque<InternalOrder> queue = book.get(order.getPrice());
        if (queue != null) {
            queue.removeIf(o -> o.getClOrderId().equals(order.getClOrderId()));
            if (queue.isEmpty()) {
                book.remove(order.getPrice());
            }
        }
    }
}

