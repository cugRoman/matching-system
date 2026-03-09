package com.stocktrade.matchingsystem;

import com.stocktrade.matchingsystem.common.constants.OrderStatus;
import com.stocktrade.matchingsystem.common.model.InternalOrder;
import com.stocktrade.matchingsystem.common.model.dto.ExecReportDTO;
import com.stocktrade.matchingsystem.common.model.dto.OrderDTO;
import com.stocktrade.matchingsystem.common.model.entity.ExecutionEntity;
import com.stocktrade.matchingsystem.common.model.entity.ReportEntity;
import com.stocktrade.matchingsystem.persistence.repository.ExecutionRepository;
import com.stocktrade.matchingsystem.persistence.repository.OrderRepository;
import com.stocktrade.matchingsystem.persistence.repository.ReportRepository;
import com.stocktrade.matchingsystem.service.OrderProcessResult;
import com.stocktrade.matchingsystem.service.OrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrchestratorServiceTests {

    @Autowired
    private OrchestratorService orchestratorService;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ExecutionRepository executionRepository;
    @Autowired
    private OrderRepository orderRepository;

    // TC1: 对敲场景
    // 1) A 买单先入场并外送交易所
    // 2) 同股东同日反向的 B 卖单到达后应触发对敲拒绝
    // 3) A 已外送订单应被撤回并最终变为 CANCELED
    @Test
    void washTradeRejectsAndCancelsExternalCounter() {
        OrderDTO a = baseOrder("A1", "B", "SH1", LocalDate.now(), 100, 10.0);
        OrderProcessResult aResult = orchestratorService.processOrder(a);
        assertThat(aResult.getType()).isEqualTo(OrderProcessResult.Type.ACCEPTED);

        OrderDTO b = baseOrder("B1", "S", "SH1", LocalDate.now(), 100, 10.0);
        OrderProcessResult bResult = orchestratorService.processOrder(b);

        assertThat(bResult.getType()).isEqualTo(OrderProcessResult.Type.REJECTED_WASH);
        assertThat(bResult.getCancelConfirms()).hasSize(1);

        InternalOrder orderA = orchestratorService.getOrder("A1").orElseThrow();
        assertThat(orderA.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    // TC2: 内部撮合 + 撤单场景
    // 1) A 买单先外送
    // 2) 非同股东 B 卖单到达后发生内部成交
    // 3) 一笔成交会产生买/卖两条执行回报（共 2 条）
    // 4) 被内部成交消耗的 A 外送挂单应被撤单并变为 CANCELED
    @Test
    void internalMatchCancelsExternalCounter() {
        OrderDTO a = baseOrder("A2", "B", "SH1", LocalDate.now(), 100, 10.0);
        orchestratorService.processOrder(a);

        OrderDTO b = baseOrder("B2", "S", "SH2", LocalDate.now(), 100, 9.5);
        OrderProcessResult bResult = orchestratorService.processOrder(b);

        assertThat(bResult.getType()).isEqualTo(OrderProcessResult.Type.ACCEPTED);
        assertThat(bResult.getExecReports()).hasSize(2);
        assertThat(bResult.getCancelConfirms()).hasSize(1);

        InternalOrder orderA = orchestratorService.getOrder("A2").orElseThrow();
        InternalOrder orderB = orchestratorService.getOrder("B2").orElseThrow();
        assertThat(orderA.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(orderB.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    // TC3: 不可成交外送场景
    // 买价低于卖价，不满足撮合价格条件：
    // 1) 不应产生执行回报
    // 2) 不应触发撤单
    // 3) 双方都保持 EXCH_WORKING
    @Test
    void noMatchBothRemainOnExchange() {
        OrderDTO a = baseOrder("A3", "B", "SH1", LocalDate.now(), 100, 9.0);
        orchestratorService.processOrder(a);

        OrderDTO b = baseOrder("B3", "S", "SH2", LocalDate.now(), 100, 10.0);
        OrderProcessResult bResult = orchestratorService.processOrder(b);

        assertThat(bResult.getExecReports()).isEmpty();
        assertThat(bResult.getCancelConfirms()).isEmpty();

        InternalOrder orderA = orchestratorService.getOrder("A3").orElseThrow();
        InternalOrder orderB = orchestratorService.getOrder("B3").orElseThrow();
        assertThat(orderA.getStatus()).isEqualTo(OrderStatus.EXCH_WORKING);
        assertThat(orderB.getStatus()).isEqualTo(OrderStatus.EXCH_WORKING);
    }

    // TC4: 跨日自动失效场景
    // tradeDate 为昨日且当前状态 EXCH_WORKING 的订单，
    // 调用 expireOrders(today) 后应直接转为 CANCELED，并返回 1 条取消确认
    @Test
    void expireOrdersCancelsPreviousTradeDate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        OrderDTO a = baseOrder("A4", "B", "SH1", yesterday, 100, 10.0);
        orchestratorService.processOrder(a);

        assertThat(orchestratorService.expireOrders(LocalDate.now())).hasSize(1);

        InternalOrder orderA = orchestratorService.getOrder("A4").orElseThrow();
        assertThat(orderA.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    // 构造基础订单，避免每个测试重复填写公共字段
    private OrderDTO baseOrder(String clOrderId, String side, String shareholderId,
            LocalDate tradeDate, int qty, double price) {
        return OrderDTO.builder()
                .clOrderId(clOrderId)
                .market("XSHG")
                .securityId("600000")
                .side(side)
                .qty(qty)
                .price(price)
                .shareHolderId(shareholderId)
                .tradeDate(tradeDate)
                .build();
    }

    // 综合流程测试：
    // 多用户、多价位、多数量下，验证 Price-Time(FIFO)、成交价、execId 成对、撤单和剩余挂单是否符合预期
    @Test
    void comprehensiveFlowValidatesFifoPriceQtyCancelAndOrderBook() {
        LocalDate today = LocalDate.now();

        // 先挂三笔卖单（形成可撮合队列）
        assertThat(orchestratorService.processOrder(baseOrder("S1", "S", "SH-S1", today, 200, 10.0)).getType())
                .isEqualTo(OrderProcessResult.Type.ACCEPTED);
        assertThat(orchestratorService.processOrder(baseOrder("S2", "S", "SH-S2", today, 100, 9.8)).getType())
                .isEqualTo(OrderProcessResult.Type.ACCEPTED);
        assertThat(orchestratorService.processOrder(baseOrder("S3", "S", "SH-S3", today, 100, 10.0)).getType())
                .isEqualTo(OrderProcessResult.Type.ACCEPTED);

        // 买单 500 进入：应按价格优先 + 同价FIFO撮合 S2(9.8) -> S1(10) -> S3(10)
        OrderProcessResult buyResult = orchestratorService.processOrder(baseOrder("B1", "B", "SH-B1", today, 500, 10.0));
        assertThat(buyResult.getType()).isEqualTo(OrderProcessResult.Type.ACCEPTED);
        assertThat(buyResult.getExecReports()).hasSize(6);
        assertThat(buyResult.getCancelConfirms()).hasSize(3);

        // 执行回报应全部标记 INTERNAL
        assertThat(buyResult.getExecReports())
                .extracting(ExecReportDTO::getExecSource)
                .containsOnly("INTERNAL");

        // 买方回报顺序校验（按撮合顺序）
        List<ExecReportDTO> buyExecs = buyResult.getExecReports().stream()
                .filter(r -> "B1".equals(r.getClOrderId()))
                .toList();
        assertThat(buyExecs).hasSize(3);
        assertThat(buyExecs).extracting(ExecReportDTO::getExecQty).containsExactly(100, 200, 100);
        assertThat(buyExecs).extracting(ExecReportDTO::getExecPrice).containsExactly(9.8, 10.0, 10.0);

        // 卖方回报顺序校验（价格优先 + 同价FIFO）
        List<ExecReportDTO> sellExecs = buyResult.getExecReports().stream()
                .filter(r -> !"B1".equals(r.getClOrderId()))
                .toList();
        assertThat(sellExecs).hasSize(3);
        assertThat(sellExecs).extracting(ExecReportDTO::getClOrderId).containsExactly("S2", "S1", "S3");

        // 每个 execId 应对应买卖两条回报（同一个成交编号）
        Map<String, Long> execIdCount = buyResult.getExecReports().stream()
                .collect(Collectors.groupingBy(ExecReportDTO::getExecId, Collectors.counting()));
        assertThat(execIdCount).hasSize(3);
        assertThat(execIdCount.values()).containsOnly(2L);

        InternalOrder b1 = orchestratorService.getOrder("B1").orElseThrow();
        InternalOrder s1 = orchestratorService.getOrder("S1").orElseThrow();
        InternalOrder s2 = orchestratorService.getOrder("S2").orElseThrow();
        InternalOrder s3 = orchestratorService.getOrder("S3").orElseThrow();

        // B1 剩余 100 未成交，继续挂交易所
        assertThat(b1.getStatus()).isEqualTo(OrderStatus.EXCH_WORKING);
        assertThat(b1.getQtyFilled()).isEqualTo(400);
        assertThat(b1.getQtyRemaining()).isEqualTo(100);
        assertThat(b1.isSentToExchange()).isTrue();

        // 被内部撮合消耗的卖方外送单均应撤销
        assertThat(s1.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(s2.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(s3.getStatus()).isEqualTo(OrderStatus.CANCELED);

        // 持久化侧校验：execution 与 report 内容一致
        List<ExecutionEntity> executions = executionRepository.findAll();
        assertThat(executions).hasSize(6);
        assertThat(executions).extracting(ExecutionEntity::getExecType).containsOnly("INTERNAL");

        List<ReportEntity> reports = reportRepository.findAll();
        Map<String, Long> reportTypeCount = reports.stream()
                .collect(Collectors.groupingBy(ReportEntity::getType, Collectors.counting()));
        assertThat(reportTypeCount.getOrDefault("CONFIRM", 0L)).isEqualTo(4L);
        assertThat(reportTypeCount.getOrDefault("EXECUTION", 0L)).isEqualTo(6L);
        assertThat(reportTypeCount.getOrDefault("CANCEL_ACK", 0L)).isEqualTo(3L);
        assertThat(reportTypeCount.getOrDefault("REJECT", 0L)).isEqualTo(0L);
    }

    // 类型与状态覆盖测试：
    // 覆盖 Type(ACCEPTED/REJECTED_INVALID/REJECTED_WASH) 与主要可达状态，
    // 并验证当前实现下不应出现的状态/回报类型未出现
    @Test
    void coversTypesAndReachableStatusesAndVerifiesUnreachableOnes() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // REJECTED_INVALID: 数量非整手
        OrderProcessResult invalid = orchestratorService.processOrder(
                baseOrder("INV1", "B", "SH-INV", today, 150, 10.0));
        assertThat(invalid.getType()).isEqualTo(OrderProcessResult.Type.REJECTED_INVALID);

        // ACCEPTED + EXCH_WORKING
        OrderProcessResult resting = orchestratorService.processOrder(
                baseOrder("R1", "B", "SH-R1", today, 100, 10.0));
        assertThat(resting.getType()).isEqualTo(OrderProcessResult.Type.ACCEPTED);

        // REJECTED_WASH
        OrderProcessResult wash = orchestratorService.processOrder(
                baseOrder("W1", "S", "SH-R1", today, 100, 10.0));
        assertThat(wash.getType()).isEqualTo(OrderProcessResult.Type.REJECTED_WASH);

        // FILLED（来单吃掉对手）
        orchestratorService.processOrder(baseOrder("M1", "S", "SH-M1", today, 100, 9.9));
        OrderProcessResult fill = orchestratorService.processOrder(baseOrder("F1", "B", "SH-F1", today, 100, 10.0));
        assertThat(fill.getType()).isEqualTo(OrderProcessResult.Type.ACCEPTED);
        assertThat(orchestratorService.getOrder("F1").orElseThrow().getStatus()).isEqualTo(OrderStatus.FILLED);

        // 跨日过期 -> CANCELED
        orchestratorService.processOrder(baseOrder("E1", "B", "SH-E1", yesterday, 100, 8.8));
        assertThat(orchestratorService.expireOrders(today)).isNotEmpty();
        assertThat(orchestratorService.getOrder("E1").orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELED);

        // 留下一笔不可成交挂单，确保 EXCH_WORKING 状态被覆盖到
        orchestratorService.processOrder(baseOrder("X1", "B", "SH-X1", today, 100, 1.0));

        // 状态覆盖（可达）
        Set<OrderStatus> statuses = orchestratorService.getAllOrders().stream()
                .map(InternalOrder::getStatus)
                .collect(Collectors.toSet());
        assertThat(statuses).contains(OrderStatus.EXCH_WORKING, OrderStatus.REJECTED_WASH, OrderStatus.FILLED, OrderStatus.CANCELED);

        // 当前实现下不可达/不保留为最终态的状态
        assertThat(statuses).doesNotContain(
                OrderStatus.NEW,
                OrderStatus.ACTIVE_IN_POOL,
                OrderStatus.PARTIALLY_FILLED,
                OrderStatus.CANCEL_REQUESTED,
                OrderStatus.CANCEL_REJECTED,
                OrderStatus.REJECTED_INVALID);

        // 回报类型覆盖：应出现 CONFIRM/REJECT/EXECUTION/CANCEL_ACK，不应出现 CANCEL_REJECT
        Set<String> reportTypes = reportRepository.findAll().stream()
                .map(ReportEntity::getType)
                .collect(Collectors.toSet());
        assertThat(reportTypes).contains("CONFIRM", "REJECT", "EXECUTION", "CANCEL_ACK");
        assertThat(reportTypes).doesNotContain("CANCEL_REJECT");
    }
}
