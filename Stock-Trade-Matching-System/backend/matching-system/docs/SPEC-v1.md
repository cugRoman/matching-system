# 股票撮合系统规范 v1

## 0. 适用范围
本文件定义 v1 行为。
文中 v2 内容仅作为备注需求，不改变 v1 运行规则。

---

## 1. 系统目标
实现一个交易系统 Demo，具备以下能力：
- 对敲检测（同股东、同交易日、反向买卖）
- 内部撮合优先（先内部撮合，再考虑外送）
- 未成交余量外送交易所
- 内部成交消耗外送对手单后，触发撤单
- 跨日自动失效（EOD 处理）
- v1 采用 A 股整手规则（仅支持整手）

---

## 2. 核心处理模式（内部撮合优先）
订单处理顺序：
1. 接收订单
2. 参数与业务校验
3. 对敲检测
4. 内部撮合（价格优先、时间优先）
5. 余量外送交易所
6. 若外送对手单被内部成交消耗，则发起撤单
7. 日终处理上一交易日未完成外送订单

---

## 3. 关键数据结构
### 3.1 Order
字段：
- `clOrderId`（String，唯一）
- `shareHolderId`（String）
- `tradeDate`（LocalDate）
- `market`（String）
- `securityId`（String）
- `side`（`B`/`S`）
- `qty`（Integer）
- `price`（Double）
- `recvTime`（Instant）
- `status`（见状态机）
- `sentToExchange`（Boolean）
- `qtyFilled`（Integer）
- `qtyRemaining`（Integer）

约束：
`qtyRemaining = qty - qtyFilled`

### 3.2 Report
字段：
- `reportId`
- `type`：`CONFIRM | REJECT | EXECUTION | CANCEL_ACK | CANCEL_REJECT`
- `refClOrdId`
- `ts`

`REJECT`：
- `rejectCode`
- `rejectText`

`EXECUTION`：
- `execId`
- `execQty`
- `execPrice`
- `execSource`：`INTERNAL | EXCHANGE`

`CANCEL_ACK`：
- `origClOrdId`
- `cancelText`

---

## 4. v1 数量规则（A 股整手）
`LOT = 100`

下单规则：
- BUY：`qty % 100 == 0`
- SELL：`qty % 100 == 0`

否则：
- `status -> REJECTED_INVALID`
- 生成 `REJECT`

说明：
- v1 不支持零股卖出清仓等扩展逻辑。

---

## 5. 对敲规则（Wash Trade）
Wash Key：
`(tradeDate, shareHolderId, market, securityId)`

当新订单 `O` 到达时，若存在对手订单 `O*` 同时满足：
- 同 `tradeDate`
- 同 `shareHolderId`
- 同 `market`
- 同 `securityId`
- `side` 相反

则：
- `O.status = REJECTED_WASH`
- 生成 `REJECT`
- `O` 不外送

附加处理：
- 若 `O*` 当前为 `EXCH_WORKING`，对 `O*` 发起撤单（`CANCEL_REQUESTED -> CANCELED`）。

---

## 6. 内部撮合规则
### 6.1 可成交条件
- BUY 对 SELL：`buy.price >= sell.price`

### 6.2 对手单选择（价格优先、时间优先）
- BUY 吃 SELL：卖价低优先；同价按 FIFO
- SELL 吃 BUY：买价高优先；同价按 FIFO

### 6.3 成交价规则（Maker Price）
- taker BUY：`execPrice = makerSell.price`
- taker SELL：`execPrice = makerBuy.price`

### 6.4 成交量规则（v1 整手）
理论成交量：
`x = min(taker.qtyRemaining, maker.qtyRemaining)`

整手修正：
`execQty = floor(x / 100) * 100`

若 `execQty == 0`：
- 本轮不成交

循环结束条件：
- 来单 `qtyRemaining == 0`，或
- 无可成交对手

---

## 7. 外送规则
内部撮合后，若：
`qtyRemaining > 0`

则：
- `status = EXCH_WORKING`
- `sentToExchange = true`
- 调用 `ExchangeService.forward()`

说明：
- 外送挂单仍可参与后续内部撮合。

---

## 8. 撤单规则
### 8.1 内部撮合触发撤单
若被命中的对手订单满足：
- `sentToExchange == true`
- `status == EXCH_WORKING`

则：
- `status -> CANCEL_REQUESTED`
- 调用 `ExchangeService.cancel()`
- 收到确认后：`status -> CANCELED`，并生成 `CANCEL_ACK`

### 8.2 跨日自动失效（EOD）
条件：
- `status == EXCH_WORKING`
- `tradeDate < todayTradeDate`

动作：
- `status -> CANCELED`
- 生成 `CANCEL_ACK`
- `cancelText = "Auto expired at end of trading day"`

注意：
- 该路径不调用 `ExchangeService.cancel()`。

---

## 9. 状态机
状态集合：
- `NEW`
- `REJECTED_INVALID`
- `REJECTED_WASH`
- `ACTIVE_IN_POOL`
- `EXCH_WORKING`
- `PARTIALLY_FILLED`
- `FILLED`
- `CANCEL_REQUESTED`
- `CANCELED`
- `CANCEL_REJECTED`

v1 说明：
- `PARTIALLY_FILLED` 在当前实现中非必需持久终态。

---

## 10. 编排顺序
新订单 `O` 到达：
1. Validate
- 不合法：`REJECTED_INVALID -> REJECT`，结束
- 合法：生成 `CONFIRM`
2. Wash Check
- 命中对敲：`REJECTED_WASH -> REJECT`；必要时撤对手外送单；结束
3. Internal Match
- 找候选
- 做价格判断
- 生成 `EXECUTION`
- 更新 `qtyFilled/qtyRemaining`
- 必要时撤外送对手单
4. Forward Remainder
- 若 `qtyRemaining > 0`：`EXCH_WORKING` 并外送

---

## 11. v1 最小测试用例
- TC1：对敲拒单 + 撤销已外送对手单
- TC2：内部成交 + 撤销已外送对手单
- TC3：价格不交叉，双方保持 `EXCH_WORKING`
- TC4：跨日失效，订单变 `CANCELED` 并生成 `CANCEL_ACK`

---

## 12. 工程约束
- 业务逻辑集中在：
  - `RiskService`
  - `MatchService`
  - `ExchangeService`
  - `OrchestratorService`
- Controller 不承载核心交易逻辑
- 所有 Report 必须持久化
- 所有状态变化必须可追踪
- 内部撮合逻辑必须有单测覆盖
- 开发环境可用 H2，生产环境建议 PostgreSQL

---

## 13. 不在 v1 范围
- 市价单
- 持仓系统
- 资金/账户风控
- 零股处理
- 分布式分片
- 高并发性能优化
- 多标的并行撮合架构重构

---

## 14. v2 需求备注（仅备注，不属于 v1 实现）
### 14.1 零股成交
- 支持 `qty % 100 != 0` 的零股场景。
- 明确 BUY/SELL 的零股下单约束。
- 定义整手与零股混合成交优先级与拆分规则。
- 补充测试：纯零股成交、整零混合成交、余量处理。

### 14.2 持仓系统
- 按 `shareHolderId + market + securityId` 建立持仓台账。
- 至少维护：
  - `totalQty`
  - `availableQty`
  - `frozenQty`
  - `todayBuyQty`（如需支持 T+1 规则）
- SELL 下单前需校验可用持仓。
- 下单/撤单/成交时，冻结与可用数量一致更新。
- 持仓变化需持久化并可审计。
- 提供持仓查询与持仓流水查询接口。

---

## 15. v1 定稿原则
- 规则优先于实现
- 状态机优先于 Controller
- 所有行为必须可测试
- 不允许隐式状态跳转