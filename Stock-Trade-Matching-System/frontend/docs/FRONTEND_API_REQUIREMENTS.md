# 股票撮合系统前端对接接口需求（v1）

## 1. 目标范围

前端需要完成以下能力：
- 用户注册/登录
- 提交订单
- 查看最近行情成交数量和价格
- 查看自己的订单
- 查看每个订单的成交情况（逐笔成交 + 汇总）

后端已提供并完善对应接口，见下文。

## 2. 通用约定

### 2.1 基础信息
- Base URL：`http://{host}:8080`
- Content-Type：`application/json`
- 时间字段：ISO-8601 字符串（UTC）

### 2.2 鉴权
- 登录/注册成功后返回 `token`
- 需要登录的接口统一携带 Header：`Authorization: Bearer {token}`
- token 过期或无效返回 `401 Unauthorized`

### 2.3 通用错误码
- `400` 请求参数错误（字段缺失/格式非法）
- `401` 未登录或 token 无效
- `404` 资源不存在（例如订单不属于当前用户或订单号不存在）

## 3. 接口清单（前端核心）

### 3.1 注册
- `POST /api/auth/register`
- 是否鉴权：否

请求：
```json
{
  "loginId": "user01",
  "password": "p123456"
}
```

成功响应（201）：
```json
{
  "token": "xxxx",
  "loginId": "user01",
  "shareHolderId": "SH_000001",
  "role": "USER",
  "expiresAtEpochMs": 1771940000000
}
```

### 3.2 登录
- `POST /api/auth/login`
- 是否鉴权：否

请求：
```json
{
  "loginId": "user01",
  "password": "p123456"
}
```

成功响应（200）：同注册返回结构。  
失败（401）：
```json
{
  "message": "Invalid credentials"
}
```

### 3.3 提交订单
- `POST /api/orders`
- 是否鉴权：是

请求：
```json
{
  "market": "XSHG",
  "securityId": "600000",
  "side": "B",
  "qty": 100,
  "price": 10.5,
  "tradeDate": "2026-02-24"
}
```

说明：
- `securityId` 必须 6 位数字
- `side`：`B`（买）/`S`（卖）
- `qty` 必须 > 0
- `price` 必须 > 0
- `tradeDate` 可不传，不传默认当天

成功响应（200）：
```json
{
  "clOrderId": "ORD_000000001",
  "resultType": "ACCEPTED",
  "message": "Order accepted"
}
```

`resultType` 可能值：
- `ACCEPTED`
- `REJECTED_INVALID`
- `REJECTED_WASH`

### 3.4 最近行情成交快照
- `GET /api/market/recent-executions`
- 是否鉴权：否

成功响应（200）：
```json
[
  {
    "market": "XSHG",
    "securityId": "600000",
    "execId": "EXE_000001",
    "execQty": 100,
    "execPrice": 10.5,
    "executedAt": "2026-02-24T14:30:12.123Z",
    "executionCount": 36
  }
]
```

字段含义：
- `execQty`/`execPrice`：该证券最近一笔成交的数量和价格
- `executionCount`：该证券累计成交笔数（系统内）

### 3.5 我的订单列表（新增，前端推荐使用）
- `GET /api/me/orders`
- 是否鉴权：是
- 查询参数（可选）：`market`、`securityId`、`status`

示例：
- `/api/me/orders`
- `/api/me/orders?market=XSHG&securityId=600000`
- `/api/me/orders?status=EXCH_WORKING`

成功响应（200）：
```json
[
  {
    "clOrderId": "ORD_000000001",
    "market": "XSHG",
    "securityId": "600000",
    "side": "B",
    "qty": 500,
    "price": 10.5,
    "status": "EXCH_WORKING",
    "qtyFilled": 400,
    "qtyRemaining": 100,
    "recvTime": "2026-02-24T14:30:00.123Z",
    "updatedAt": "2026-02-24T14:31:00.123Z"
  }
]
```

### 3.6 我的订单成交明细（新增，前端推荐使用）
- `GET /api/me/orders/{clOrderId}/executions`
- 是否鉴权：是

成功响应（200）：
```json
{
  "clOrderId": "ORD_000000001",
  "market": "XSHG",
  "securityId": "600000",
  "side": "B",
  "qty": 500,
  "qtyFilled": 400,
  "qtyRemaining": 100,
  "status": "EXCH_WORKING",
  "executionCount": 3,
  "totalExecutedQty": 400,
  "avgExecutedPrice": 10.1,
  "lastExecutedAt": "2026-02-24T14:31:00.123Z",
  "executions": [
    {
      "execId": "EXE_000003",
      "execQty": 100,
      "execPrice": 10.0,
      "execType": "INTERNAL",
      "counterOrderId": null,
      "executedAt": "2026-02-24T14:31:00.123Z"
    }
  ]
}
```

失败（404）：
```json
{
  "message": "Order not found"
}
```

> 该 404 同时表示：订单不存在，或该订单不属于当前登录用户。

### 3.7 我的按股票聚合活动（已有）
- `GET /api/me/stocks/activity`
- 是否鉴权：是
- 查询参数（可选）：`market`、`securityId`

说明：
- 返回按 `market + securityId` 聚合的订单与回报列表
- 适合“我的持仓活动总览页”或“按股票分组看订单/回报”

## 4. 关键枚举字典

### 4.1 `side`
- `B`：买
- `S`：卖

### 4.2 `status`（订单状态）
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

### 4.3 回报类型 `type`（在 `/api/me/stocks/activity` 的 reports 中）
- `CONFIRM`
- `REJECT`
- `EXECUTION`
- `CANCEL_ACK`
- `CANCEL_REJECT`

## 5. 前端页面与接口映射建议

- 登录页/注册页
  - `POST /api/auth/login`
  - `POST /api/auth/register`

- 下单页
  - `POST /api/orders`

- 行情看板（最近成交）
  - `GET /api/market/recent-executions`（轮询 2~5 秒）

- 我的订单列表页
  - `GET /api/me/orders`
  - 支持通过 query 参数做市场/证券/状态筛选

- 订单详情页（成交明细）
  - `GET /api/me/orders/{clOrderId}/executions`

## 6. 可选实时推送（WebSocket）

如前端需要减少轮询，可订阅：
- 连接端点：`/ws`（SockJS/STOMP）
- 主题：
  - `/topic/reports/confirm`
  - `/topic/reports/reject`
  - `/topic/reports/execution`
  - `/topic/reports/cancel`
  - `/topic/alerts/washtrade`

建议：
- 订单页先用 REST 拉取初始数据
- 再用 WebSocket 增量更新（新增成交、拒单、撤单）
