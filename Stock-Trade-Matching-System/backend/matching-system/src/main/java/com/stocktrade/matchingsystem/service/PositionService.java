package com.stocktrade.matchingsystem.service;

import com.stocktrade.matchingsystem.common.model.entity.PositionEntity;
import com.stocktrade.matchingsystem.persistence.repository.PositionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;

    /**
     * 获取某股东的所有持仓
     */
    public List<PositionEntity> getPositionsByShareHolderId(String shareHolderId) {
        return positionRepository.findByShareHolderId(shareHolderId);
    }

    /**
     * 系统启动时，自动注入几条测试持仓数据（为了方便前端测试）
     * 等真正和订单撮合打通后，真实的持仓是通过交易计算出来的
     */
    /*
    @PostConstruct
    public void initMockData() {
        if (positionRepository.count() == 0) {
            log.info("初始化假持仓数据...");
            positionRepository.save(PositionEntity.builder()
                    .shareHolderId("SH_100001")
                    .market("XSHG")
                    .securityId("600519") // 贵州茅台
                    .totalQty(10000)
                    .availableQty(10000)
                    .frozenQty(0)
                    .build());

            positionRepository.save(PositionEntity.builder()
                    .shareHolderId("SH_100001")
                    .market("XSHE")
                    .securityId("000001") // 平安银行
                    .totalQty(5000)
                    .availableQty(4000)
                    .frozenQty(1000) // 模拟有1000股被挂单冻结了
                    .build());
        }
    }
    */

    /**
     * 卖出时冻结持仓
     * @return true: 冻结成功; false: 持仓不足或无持仓
     */
    @Transactional // 加上事务注解，保证数据一致性
    public boolean freezePositionForSell(String shareHolderId, String market, String securityId, int qty) {
        Optional<PositionEntity> opt = positionRepository.findByShareHolderIdAndSecurityId(shareHolderId, securityId);
        
        // 1. 如果连这只股票都没有，直接失败
        if (opt.isEmpty()) {
            return false;
        }
        
        PositionEntity pos = opt.get();
        
        // 2. 如果可用数量小于想卖的数量，失败
        if (pos.getAvailableQty() < qty) {
            return false;
        }
        
        // 3. 扣减可用，增加冻结
        pos.setAvailableQty(pos.getAvailableQty() - qty);
        pos.setFrozenQty(pos.getFrozenQty() + qty);
        
        // 4. 保存回数据库
        positionRepository.save(pos);
        return true;
    }

    /**
     * 规则1 & 2：处理成交回报（买入增加总数，卖出扣除冻结与总数）
     */
    @Transactional
    public void handleExecution(String shareHolderId, String market, String securityId, String side, int execQty) {
        Optional<PositionEntity> opt = positionRepository.findByShareHolderIdAndSecurityId(shareHolderId, securityId);

        PositionEntity pos;
        if (opt.isEmpty()) {
            // 如果是买入了一只以前从未拥有过的新股票，需要新建一条持仓记录
            if ("B".equalsIgnoreCase(side)) {
                pos = PositionEntity.builder()
                        .shareHolderId(shareHolderId)
                        .market(market)
                        .securityId(securityId)
                        .totalQty(0)
                        .availableQty(0)
                        .frozenQty(0)
                        .build();
            } else {
                log.error("数据异常：卖出成交时找不到持仓记录 {}", shareHolderId);
                return;
            }
        } else {
            pos = opt.get();
        }

        if ("B".equalsIgnoreCase(side)) {
            // 买入成交：只增加总持仓 (A股 T+1 制度，今天买的明天才能卖，所以 availableQty 不增加)
            pos.setTotalQty(pos.getTotalQty() + execQty);
        } else if ("S".equalsIgnoreCase(side)) {
            // 卖出成交：扣减总持仓和冻结持仓
            pos.setTotalQty(pos.getTotalQty() - execQty);
            pos.setFrozenQty(pos.getFrozenQty() - execQty);
        }

        positionRepository.save(pos);
    }

    /**
     * 规则3：处理撤单与拒绝（卖单解冻）
     */
    @Transactional
    public void handleCancel(String shareHolderId, String securityId, String side, int canceledQty) {
        // 只有卖单撤销，才需要解冻股票（买单撤销解冻的是资金，不归我们持仓系统管）
        if ("S".equalsIgnoreCase(side)) {
            positionRepository.findByShareHolderIdAndSecurityId(shareHolderId, securityId).ifPresent(pos -> {
                pos.setFrozenQty(pos.getFrozenQty() - canceledQty);
                pos.setAvailableQty(pos.getAvailableQty() + canceledQty);
                positionRepository.save(pos);
            });
        }
    }
}

