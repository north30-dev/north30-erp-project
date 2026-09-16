接口设计遵循 **RESTful** 风格，**统一返回格式**，并充分考虑ERP业务场景（幂等性、状态流转、批量操作）的特殊要求。

---

# ERP系统API接口设计文档（V1.0）

| 文档属性 | 内容 |
|---------|------|
| 文档名称 | ERP系统API接口设计文档 |
| 版本号 | V1.0 |
| 生效日期 | 2026年9月1日 |
| 接口协议 | HTTP/HTTPS |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 基础路径 | `/api` |


## 一、接口设计总纲

### 1.1 统一返回格式

所有接口返回统一的JSON结构 `Result<T>`：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": 1725000000000,
  "traceId": "abc123xyz"    // 可选，分布式链路追踪ID
}
```

**状态码说明**：

| code值 | 含义 | 说明 |
|--------|------|------|
| 200 | 成功 | 请求处理成功 |
| 400 | 参数错误 | 必填为空、格式错误、业务校验不通过 |
| 401 | 未认证 | Token缺失或过期 |
| 403 | 无权限 | 用户无此功能权限 |
| 404 | 资源不存在 | 请求的资源ID不存在 |
| 409 | 数据冲突 | 乐观锁冲突、重复提交 |
| 422 | 业务校验失败 | 库存不足、状态不匹配等业务规则限制 |
| 500 | 系统错误 | 服务器内部异常 |

### 1.2 公共请求头

| Header | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `Authorization` | String | 是（除登录接口） | Bearer Token，格式 `Bearer {jwt_token}` |
| `Content-Type` | String | 是 | `application/json` |
| `Accept-Language` | String | 否 | 语言偏好：zh-CN / en-US |
| `X-Request-Id` | String | 否 | 请求追踪ID，用于链路追踪 |

### 1.3 分页参数规范

所有列表查询接口统一使用以下分页参数：

| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `pageNum` | Integer | 否 | 1 | 页码，从1开始 |
| `pageSize` | Integer | 否 | 20 | 每页条数，最大不超过200 |
| `orderBy` | String | 否 | `create_time` | 排序字段 |
| `orderDirection` | String | 否 | `DESC` | 排序方向：ASC / DESC（排序字段必须经服务端白名单校验，防止 SQL 注入） |

**分页响应结构**：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [ ... ],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20,
    "pages": 5
  }
}
```

### 1.4 鉴权机制

所有接口（除登录、验证码外）均需携带 `Authorization: Bearer {jwt_token}`。

- **Token有效期**：2小时
- **刷新机制**：提供独立的刷新接口 `/auth/refresh`
- **权限粒度**：接口级别 + 数据级别（按钮级权限由前端控制）

### 1.5 幂等性要求（重要）

以下类型的接口必须支持**幂等性**（重复请求返回相同结果，不产生副作用）：

- 创建订单/工单（使用客户端生成的 `idempotent-key` 防重提交）
- 审核/驳回操作
- 确认收货/发货

**实现方式**：客户端在请求头中携带 `Idempotent-Key: {uuid}`，服务端用Redis缓存处理结果（过期时间24小时）。


## 二、模块一：认证授权（Auth）

**基础路径**：`/api/auth`

### 2.1 用户登录

**接口**：`POST /auth/login`

**请求体**：

```json
{
  "username": "admin",
  "password": "123456",
  "captcha": "A3B8",
  "captchaKey": "abcd-1234-xyz"   // 验证码唯一标识
}
```

**响应数据**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 7200,
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "userInfo": {
      "userId": 1,
      "username": "admin",
      "realName": "系统管理员",
      "roles": ["admin", "manager"]
    }
  }
}
```

### 2.2 刷新Token

**接口**：`POST /auth/refresh`

**请求体**：

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

### 2.3 用户登出

**接口**：`POST /auth/logout`

**请求头**：`Authorization: Bearer {token}`


## 三、模块二：物料与BOM管理（Material）

**基础路径**：`/api/materials`

### 3.1 物料分页查询

**接口**：`GET /materials`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `materialCode` | String | 否 | 物料编码（模糊查询） |
| `materialName` | String | 否 | 物料名称（模糊查询） |
| `categoryId` | Long | 否 | 物料分类ID |
| `materialType` | Integer | 否 | 1-原材料 2-半成品 3-成品 |
| `status` | Integer | 否 | 0-新建 1-审核 2-启用 3-停用 |
| `pageNum` | Integer | 否 | 页码 |
| `pageSize` | Integer | 否 | 每页条数 |

### 3.2 物料详情查询

**接口**：`GET /materials/{id}`

**响应示例**：

```json
{
  "code": 200,
  "data": {
    "id": 1001,
    "materialCode": "MAT-IC-001",
    "materialName": "STM32F103主控芯片",
    "spec": "LQFP-48",
    "categoryId": 5,
    "categoryName": "芯片类",
    "materialType": 1,
    "sourceType": 2,
    "unit": "个",
    "standardCost": 12.50,
    "safetyStock": 1000,
    "abcClass": "A",
    "status": 2,
    "createTime": "2026-08-01 10:00:00"
  }
}
```

### 3.3 物料新增

**接口**：`POST /materials`

**请求体**：

```json
{
  "materialCode": "MAT-IC-002",
  "materialName": "LM2596电源芯片",
  "spec": "TO-263",
  "categoryId": 5,
  "materialType": 1,
  "sourceType": 2,
  "unit": "个",
  "standardCost": 3.80,
  "safetyStock": 500,
  "abcClass": "B",
  "isBatchManaged": true,
  "isShelfLife": true,
  "shelfLifeDays": 730
}
```

### 3.4 BOM查询（树形结构）

**接口**：`GET /boms/{id}/tree`

**响应示例**（递归结构）：

```json
{
  "code": 200,
  "data": {
    "bomId": 2001,
    "productName": "AC-300变频器",
    "productCode": "AC-300",
    "level": 0,
    "qty": 1,
    "children": [
      {
        "materialId": 1001,
        "materialName": "主控板组件",
        "level": 1,
        "qty": 1,
        "children": [
          {
            "materialId": 2002,
            "materialName": "PCB光板",
            "level": 2,
            "qty": 1
          },
          {
            "materialId": 1001,
            "materialName": "STM32F103芯片",
            "level": 2,
            "qty": 2
          }
        ]
      },
      {
        "materialId": 3001,
        "materialName": "机箱外壳",
        "level": 1,
        "qty": 1
      }
    ]
  }
}
```


## 四、模块三：采购管理（Purchase）

**基础路径**：`/api/purchase`

### 4.1 采购订单分页查询

**接口**：`GET /orders`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `orderCode` | String | 否 | 订单号（精确匹配） |
| `supplierId` | Long | 否 | 供应商ID |
| `orderStatus` | Integer | 否 | 0-草稿 1-审核 2-部分收货 3-已收货 |
| `startDate` | String | 否 | 下单开始日期（yyyy-MM-dd） |
| `endDate` | String | 否 | 下单结束日期 |
| `pageNum` | Integer | 否 | 页码 |

### 4.2 采购订单详情

**接口**：`GET /orders/{id}`

**响应示例**：

```json
{
  "code": 200,
  "data": {
    "id": 5001,
    "orderCode": "PO202608001",
    "supplierId": 201,
    "supplierName": "宏微科技有限公司",
    "orderDate": "2026-08-15",
    "requiredDate": "2026-08-30",
    "totalAmount": 17000.00,
    "taxRate": 13.00,
    "orderStatus": 2,
    "settlementStatus": 0,
    "lines": [
      {
        "lineNo": 1,
        "materialId": 1001,
        "materialName": "IGBT模块",
        "qty": 200,
        "unitPrice": 85.00,
        "lineAmount": 17000.00,
        "receivedQty": 200
      }
    ],
    "receipts": [
      {
        "receiptCode": "RE202608001",
        "receiptQty": 200,
        "qualifiedQty": 200,
        "receiptDate": "2026-08-28 14:30:00"
      }
    ]
  }
}
```

### 4.3 创建采购订单

**接口**：`POST /orders`

**请求头**：`Idempotent-Key: {uuid}`

**请求体**：

```json
{
  "supplierId": 201,
  "requiredDate": "2026-08-30",
  "taxRate": 13.00,
  "paymentTerms": "月结30天",
  "remark": "重点交期，请确保按时到货",
  "lines": [
    {
      "materialId": 1001,
      "qty": 200,
      "unitPrice": 85.00
    },
    {
      "materialId": 1005,
      "qty": 400,
      "unitPrice": 2.30
    }
  ]
}
```

### 4.4 审核采购订单

**接口**：`POST /orders/{id}/approve`

**请求体**：

```json
{
  "approved": true,
  "remark": "价格合理，同意采购"
}
```

### 4.5 采购收货确认

**接口**：`POST /receipts`

**请求体**：

```json
{
  "purchaseOrderId": 5001,
  "warehouseId": 1,
  "receipts": [
    {
      "materialId": 1001,
      "receiptQty": 200,
      "qualifiedQty": 200,
      "rejectQty": 0,
      "batchNo": "BATCH-20260828-001",
      "productionDate": "2026-08-20"
    }
  ]
}
```

> **业务规则**：收货审核通过后，自动生成 `inv_transaction`（采购入库流水）和财务暂估凭证。


## 五、模块四：销售管理（Sales）

**基础路径**：`/api/sales`

### 5.1 销售订单创建（核心）

**接口**：`POST /orders`

**请求头**：`Idempotent-Key: {uuid}`

**请求体**：

```json
{
  "customerId": 101,
  "requiredDate": "2026-09-30",
  "paymentTerms": "月结60天",
  "taxRate": 13.00,
  "lines": [
    {
      "materialId": 3005,
      "qty": 100,
      "unitPrice": 1850.00
    }
  ],
  "deliverySchedule": [          // 分批交货计划
    {
      "deliveryDate": "2026-09-20",
      "qty": 50
    },
    {
      "deliveryDate": "2026-09-30",
      "qty": 50
    }
  ]
}
```

**响应**：

```json
{
  "code": 200,
  "data": {
    "orderId": 8001,
    "orderCode": "SO202608001",
    "totalAmount": 185000.00,
    "status": "待审核",
    "mrpPlanId": 9001,           // 关联的MRP计划ID
    "suggestPurchase": [ ... ],  // MRP生成的采购建议
    "suggestManufacture": [ ... ] // MRP生成的生产建议
  }
}
```

### 5.2 销售订单查询

**接口**：`GET /orders`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `customerId` | Long | 否 | 客户ID |
| `orderStatus` | Integer | 否 | 0-草稿 1-审核 2-部分发货 3-已发货 |
| `orderCode` | String | 否 | 订单号（模糊） |
| `pageNum` | Integer | 否 | 页码 |

### 5.3 订单执行状态跟踪

**接口**：`GET /orders/{id}/track`

**响应示例**：

```json
{
  "code": 200,
  "data": {
    "orderCode": "SO202608001",
    "orderStatus": "部分发货",
    "deliveredQty": 50,
    "totalQty": 100,
    "deliveryProgress": "50%",
    "manufactureOrders": [
      {
        "workOrderCode": "MO202608032",
        "plannedQty": 100,
        "completedQty": 80,
        "status": "生产中",
        "progress": "80%"
      }
    ],
    "shipments": [
      {
        "deliveryDate": "2026-09-20",
        "qty": 50,
        "status": "已出库"
      }
    ]
  }
}
```

### 5.4 销售发货出库

**接口**：`POST /shipments`

**请求体**：

```json
{
  "salesOrderId": 8001,
  "warehouseId": 1,
  "lines": [
    {
      "materialId": 3005,
      "qty": 50,
      "batchNo": "BATCH-20260920-001"
    }
  ]
}
```


## 六、模块五：库存管理（Inventory）

**基础路径**：`/api/inventory`

### 6.1 即时库存查询

**接口**：`GET /stock`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `materialId` | Long | 否 | 物料ID |
| `materialCode` | String | 否 | 物料编码 |
| `warehouseId` | Long | 否 | 仓库ID |
| `batchNo` | String | 否 | 批次号 |
| `pageNum` | Integer | 否 | 页码 |

**响应示例**：

```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "materialId": 1001,
        "materialCode": "MAT-IC-001",
        "materialName": "STM32F103芯片",
        "warehouseId": 1,
        "warehouseName": "苏州中心仓",
        "batchNo": "BATCH-202606-001",
        "qty": 3250,
        "frozenQty": 0,
        "lastTransactionTime": "2026-08-28 14:30:00"
      }
    ],
    "total": 1
  }
}
```

### 6.2 库存流水查询（审计追溯）

**接口**：`GET /transactions`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `materialId` | Long | 是 | 物料ID |
| `startTime` | String | 否 | 开始时间 |
| `endTime` | String | 否 | 结束时间 |
| `transType` | Integer | 否 | 流水类型 |
| `sourceBizId` | Long | 否 | 来源单据ID |
| `pageNum` | Integer | 否 | 页码 |

**响应示例**：

```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "transCode": "T20260828001",
        "transType": 10,
        "transTypeName": "采购入库",
        "inQty": 200,
        "outQty": 0,
        "beforeQty": 3100,
        "afterQty": 3300,
        "sourceBizType": "PURCHASE_RECEIPT",
        "sourceBizCode": "RE202608001",
        "transTime": "2026-08-28 14:30:00"
      }
    ]
  }
}
```

### 6.3 库存盘点

**接口**：`POST /stock-take`

**请求体**：

```json
{
  "warehouseId": 1,
  "materialId": 1001,
  "systemQty": 100.00,      // 系统账面数量
  "actualQty": 98.00,       // 实际盘点数量
  "diffQty": -2.00          // 差异（盘亏2个）
}
```

### 6.4 安全库存预警

**接口**：`GET /stock/alerts`

**响应示例**：

```json
{
  "code": 200,
  "data": [
    {
      "materialId": 1001,
      "materialCode": "MAT-IC-001",
      "materialName": "STM32F103芯片",
      "currentQty": 150,
      "safetyStock": 1000,
      "shortageQty": 850,
      "alertLevel": "紧急"
    }
  ]
}
```


## 七、模块六：生产制造管理（Manufacturing）

**基础路径**：`/api/manufacturing`

### 7.1 MRP运算触发

**接口**：`POST /mrp/run`

**请求体**：

```json
{
  "salesOrderId": 8001,              // 关联销售订单ID（MTO模式）
  "considerStock": true,             // 是否考虑现有库存
  "considerPurchase": true,          // 是否考虑在途采购
  "considerManufacture": true        // 是否考虑在制工单
}
```

**响应示例**：

```json
{
  "code": 200,
  "data": {
    "mrpPlanId": 9001,
    "purchaseSuggestions": [
      {
        "materialId": 1001,
        "materialName": "IGBT模块",
        "suggestQty": 200,
        "suggestDate": "2026-08-30",
        "supplierId": 201,
        "leadTime": 15
      }
    ],
    "manufactureSuggestions": [
      {
        "materialId": 4001,
        "materialName": "主控板组件",
        "suggestQty": 100,
        "startDate": "2026-09-01",
        "endDate": "2026-09-15"
      }
    ]
  }
}
```

### 7.2 创建生产工单

**接口**：`POST /work-orders`

**请求体**：

```json
{
  "materialId": 3005,
  "plannedQty": 100,
  "plannedStartDate": "2026-09-01",
  "plannedEndDate": "2026-09-25",
  "bomId": 2001,
  "salesOrderId": 8001
}
```

### 7.3 生产领料

**接口**：`POST /work-orders/{id}/issue`

**请求体**：

```json
{
  "issueItems": [
    {
      "materialId": 1001,
      "plannedQty": 200,
      "actualQty": 200,
      "warehouseId": 1,
      "batchNo": "BATCH-202608-001"
    }
  ]
}
```

### 7.4 工单报工（完工汇报）

**接口**：`POST /work-orders/{id}/report`

**请求体**：

```json
{
  "completedQty": 80,
  "scrappedQty": 2,
  "operator": "张三",
  "workTime": "2026-09-20 16:30:00",
  "remark": "今日完成80台，2台测试不合格已报废"
}
```


## 八、模块七：财务管理（Finance）

**基础路径**：`/api/finance`

### 8.1 应收账款账龄分析

**接口**：`GET /receivable/aging`

**响应示例**：

```json
{
  "code": 200,
  "data": {
    "customerId": 101,
    "customerName": "申达电气有限公司",
    "totalReceivable": 185000.00,
    "aging": {
      "within30": 0.00,
      "30to60": 185000.00,
      "60to90": 0.00,
      "over90": 0.00
    }
  }
}
```

### 8.2 凭证生成（业务单据过账）

> **说明**：ERP的核心机制——业务单据驱动会计凭证自动生成。

**接口**：`POST /vouchers/generate`

**请求体**：

```json
{
  "sourceBizType": "PURCHASE_RECEIPT",
  "sourceBizId": 6001
}
```

**响应示例**（生成的凭证）：

```json
{
  "code": 200,
  "data": {
    "voucherCode": "FZ202608001",
    "entries": [
      {
        "subjectCode": "1403",
        "subjectName": "原材料",
        "direction": "借",
        "amount": 17000.00
      },
      {
        "subjectCode": "2202",
        "subjectName": "应付账款-暂估",
        "direction": "贷",
        "amount": 17000.00
      }
    ]
  }
}
```

### 8.3 成本核算（月末结转）

**接口**：`POST /cost/calculate`

**请求体**：

```json
{
  "period": "202608",
  "calculateType": "monthly_average"
}
```


## 九、模块八：报表统计（Report）

**基础路径**：`/api/reports`

### 9.1 经营仪表盘

**接口**：`GET /dashboard`

**响应示例**：

```json
{
  "code": 200,
  "data": {
    "todaySales": 185000.00,
    "monthSales": 3200000.00,
    "grossProfitRate": 28.5,
    "inventoryTurnoverDays": 45,
    "orderOnTimeRate": 82.5,
    "receivableOverdue": 320000.00,
    "pendingOrderCount": 18,
    "alerts": {
      "stockLow": 5,
      "overdueReceivable": 3,
      "unapprovedOrders": 7
    }
  }
}
```

### 9.2 采购价格走势分析

**接口**：`GET /purchase/price-trend`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `materialId` | Long | 是 | 物料ID |
| `startDate` | String | 否 | 开始日期 |
| `endDate` | String | 否 | 结束日期 |


## 十、错误码定义

业务错误码统一在 `ErrorCodeEnum` 中定义，返回格式示例：

```json
{
  "code": 10001,
  "message": "库存不足，当前可用库存 150，需要 200",
  "timestamp": 1725000000000
}
```

**错误码区间分配**：

| 区间 | 模块 |
|------|------|
| 10001-10999 | 通用错误（参数校验、权限等） |
| 11001-11999 | 物料/BOM错误 |
| 12001-12999 | 采购错误 |
| 13001-13999 | 销售错误 |
| 14001-14999 | 库存错误 |
| 15001-15999 | 生产制造错误 |
| 16001-16999 | 财务错误 |


## 十一、接口设计补充规范

### 11.1 批量操作接口规范

ERP中常见批量操作，统一采用 `POST /xxx/batch` 模式：

| 接口 | 方法 | 说明 |
|------|------|------|
| `/materials/batch-delete` | POST | 批量删除物料 |
| `/orders/batch-approve` | POST | 批量审核订单 |
| `/inventory/batch-adjust` | POST | 批量库存调整 |

### 11.2 状态流转接口规范

ERP单据状态变更，统一使用 **动作接口** 而非直接修改状态字段：

| 动作 | 接口示例 | 说明 |
|------|---------|------|
| 提交审核 | `POST /orders/{id}/submit` | 草稿→待审核 |
| 审核通过 | `POST /orders/{id}/approve` | 待审核→已审核 |
| 审核驳回 | `POST /orders/{id}/reject` | 待审核→已驳回 |
| 关闭 | `POST /orders/{id}/close` | 终止单据，不再执行 |
| 反审核 | `POST /orders/{id}/unapprove` | 已审核→草稿（需特殊权限） |

### 11.3 导出接口规范

所有列表页面支持数据导出，统一使用：

| 接口 | 说明 |
|------|------|
| `GET /xxx/export` | 导出当前查询条件下的全部数据，格式Excel |

**响应头**：`Content-Disposition: attachment; filename=采购订单_202608.xlsx`


## 十二、附录：典型端到端流程接口调用示例

以“申达电气”100台变频器订单的全流程为例，展示API调用顺序：

```
1. 客户下单 → 调用 销售订单创建接口（SALES /orders）
   ↓ 返回 MRP计划ID
2. MRP运算 → 调用 MRP触发接口（MANUFACTURING /mrp/run）
   ↓ 生成采购建议+生产建议
3. 采购员下采购单 → 调用 采购订单创建接口（PURCHASE /orders）
4. 生产部下工单 → 调用 生产工单创建接口（MANUFACTURING /work-orders）
5. 供应商到货 → 调用 采购收货确认接口（PURCHASE /receipts）
   ↓ 自动生成库存流水 + 暂估凭证
6. 生产领料 → 调用 生产领料接口（MANUFACTURING /work-orders/{id}/issue）
   ↓ 自动扣减库存
7. 生产报工 → 调用 工单报工接口（MANUFACTURING /work-orders/{id}/report）
8. 成品入库 → 自动生成 生产完工入库流水
9. 销售发货 → 调用 销售发货出库接口（SALES /shipments）
   ↓ 自动生成销售出库流水 + 主营业务成本凭证
10. 财务过账 → 调用 凭证生成接口（FINANCE /vouchers/generate）
    ↓ 生成正式会计凭证
11. 客户付款 → 录入收款单，核销应收账款
```


