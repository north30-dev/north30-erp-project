本规范基于这套 **Java 21 + MyBatis-Plus 3.5.17 + Spring Boot 4.0.x（模块化单体，暂不引入 Spring Cloud）+ Vue 3 + PostgreSQL 16** 技术栈编写。

ERP系统涉及金钱（财务）和实物（库存），编码规范的核心理念是 **“防呆设计、数据零差错、全链路可追溯”**。规范必须严格，建议团队内部强制执行。

---

# ERP系统开发编码规范文档（V1.0）

| 文档属性 | 内容 |
|---------|------|
| 文档名称 | ERP系统开发编码规范 |
| 版本号 | V1.0 |
| 生效日期 | 2026年9月1日 |
| 适用范围 | 所有后端（Java）及前端（Vue）开发人员 |
| 违反后果 | 代码审查不通过，责令重构，计入绩效考核 |


## 一、总则与核心铁律

在ERP开发中，以下三条是**不可妥协的铁律**：

1. **金额计算绝对禁止使用浮点数（Float/Double）** ：涉及单价、金额、税率、折扣的计算，**必须使用 `BigDecimal`**，且指定精度（保留2位小数，四舍五入）。
2. **任何写操作（增/删/改）必须记录审计日志**：谁、什么时间、在哪个IP、修改了什么数据（变更前后快照），必须记录。
3. **数据库写操作必须显式管理事务**：增删改操作必须加 `@Transactional`，且明确指定 `rollbackFor = Exception.class`，杜绝事务不回滚导致的“财务账不平”。


## 二、后端开发规范

### 2.1 项目结构与模块划分（多模块Maven工程）

项目采用**模块化单体**架构，共 5 个 Maven 模块：`erp-common`（公共）/ `erp-system`（系统管理）/ `erp-base`（基础数据）/ `erp-business`（业务模块，内部再按 purchase / sales / inventory / manufacturing / finance 业务域分包）/ `erp-bootstrap`（唯一启动入口）。**每个模块内部**统一按以下包结构组织：

```
me.north30.erp.[模块]              # erp-system / erp-base / erp-business（erp-business 下再分业务域子包）
├── controller        // 接口层（接收请求，参数校验，返回响应）
├── service           // 业务逻辑层（接口定义）
│   └── impl          // 业务逻辑层（实现类）
├── mapper            // 数据访问层（继承MyBatis-Plus BaseMapper）
├── entity            // 数据库实体类（与表一一映射）
├── dto               // 数据传输对象（接口入参/出参，不直接暴露Entity）
├── vo                // 视图对象（复杂展示层聚合数据）
├── enums             // 枚举类（状态、类型、业务字典）
├── config            // 配置类（本地配置、拦截器配置）
├── constant          // 常量定义
└── util              // 工具类（无状态静态方法）
```

### 2.2 命名规范

| 类别 | 命名规则 | 示例 |
|------|---------|------|
| **包名** | 全小写，点分隔 | `me.north30.erp.purchase`、`me.north30.erp.inventory` |
| **类名（Class）** | 大驼峰（PascalCase） | `PurchaseOrderService`、`InventoryController` |
| **接口（Interface）** | 大驼峰，建议不加I前缀 | `IStockService`（更推荐 `StockService`） |
| **抽象类** | 加 `Abstract` 前缀 | `AbstractBaseHandler` |
| **实现类（Impl）** | 接口名 + `Impl` 后缀 | `StockServiceImpl` |
| **方法名** | 小驼峰（camelCase） | `createPurchaseOrder`、`queryStockBySku` |
| **常量** | 全大写，下划线分隔 | `MAX_BATCH_SIZE`、`DEFAULT_PAGE_SIZE` |
| **变量名** | 小驼峰 | `orderId`、`skuCode` |
| **枚举类** | 大驼峰，加 `Enum` 后缀 | `OrderStatusEnum`、`BizTypeEnum` |
| **Entity类** | 对应表名（大驼峰） | `PurOrder`（表名 `pur_order`） |
| **DTO类** | 业务名 + `DTO` 后缀 | `OrderCreateDTO`、`StockQueryDTO` |
| **VO类** | 业务名 + `VO` 后缀 | `OrderDetailVO`、`InventoryDashboardVO` |
| **Mapper接口** | Entity名 + `Mapper` | `PurchaseOrderMapper` |
| **Service接口** | 业务名 + `Service` | `PurchaseOrderService` |
| **工具类** | 功能名 + `Util` | `BigDecimalUtil`、`DateUtil` |

### 2.3 分层职责硬约束（重点）

| 层级 | 核心职责 | **严禁操作** |
|------|---------|------------|
| **Controller** | 接收请求、校验入参（@Valid）、调用Service、封装统一响应 | ❌ 禁止写业务逻辑 ❌ 禁止直接操作数据库 ❌ 禁止直接返回Entity |
| **Service** | 核心业务逻辑、事务管理、流程编排 | ❌ 禁止直接操作数据库SQL ❌ 禁止SQL拼接 |
| **Mapper** | 仅CRUD及简单关联查询 | ❌ 禁止在XML中硬编码业务常量（应使用枚举） |
| **Entity** | 纯数据映射，贫血模型 | ❌ 禁止携带业务方法 ❌ 禁止关联其他Entity |
| **DTO/VO** | 接口数据契约 | ❌ 禁止DTO中引用Entity ❌ 禁止携带数据库注解 |

**特别规范**：
- Controller 层接收请求后，**必须在入口处将 DTO 转换为 Entity/BO（业务对象）**，返回时再将 Entity 转换为 VO/DTO，**严禁直接使用 Entity 进行数据序列化返回给前端**，防止序列化时暴露敏感字段（如密码密文、乐观锁版本号）或因 `@JsonIgnore` 配置不当导致逻辑混乱。
- Controller 中业务校验失败，**直接抛出业务异常**，由统一全局异常处理器捕获并返回给前端，不要在Controller中写 `try-catch` 吃掉异常。

### 2.4 接口设计规范（API契约）

遵循 **RESTful** 风格，统一返回结构：

**统一响应体 `Result` 格式**（所有接口必须遵守）：

```json
{
  "code": 200,          // 业务状态码（200=成功，4xx=客户端错误，5xx=服务端错误）
  "message": "操作成功", // 返回信息
  "data": { ... },      // 具体数据，可为null
  "timestamp": 1725000000000,  // 时间戳
  "traceId": "abc123xyz"       // 链路追踪ID（可选）
}
```

**RESTful 路径规范**：

| 操作 | HTTP方法 | URL示例 | 说明 |
|------|---------|---------|------|
| 分页查询 | GET | `/api/v1/purchase/orders?pageNum=1&pageSize=20` | 查询列表（带分页） |
| 详情查询 | GET | `/api/v1/purchase/orders/{id}` | 查询单条记录 |
| 新增 | POST | `/api/v1/purchase/orders` | 创建资源，Body传参 |
| 修改 | PUT | `/api/v1/purchase/orders/{id}` | 全量/部分更新（推荐直接用POST亦可） |
| 删除 | DELETE | `/api/v1/purchase/orders/{id}` | 逻辑删除（配合状态字段） |
| 审核 | POST | `/api/v1/purchase/orders/{id}/approve` | 动词场景，走POST |

> **重要**：禁止在URL中出现动词的复数形式与名词混淆，所有接口路径必须带有版本号 `/api/v1/...`。

### 2.5 数据库操作规范（Mapper/MyBatis-Plus）

1. **查询规范**：
   - 单表查询必须使用 MyBatis-Plus 的 `LambdaQueryWrapper`，避免硬编码字段名（防止数据库字段改名导致编译不报错、运行报错）。
   - 多表复杂关联查询、报表统计，必须写在 Mapper.xml 中，并写清楚SQL注释。
   - **禁止在循环体内查询数据库**（N+1问题），务必使用 `IN` 批量查询或 `JOIN` 一次性查出。

2. **更新规范**：
   - 更新数据必须使用 `LambdaUpdateWrapper` 或 `UpdateWrapper` 带条件更新，**务必带上乐观锁版本号 `version` 字段**，防止并发覆盖。
   - **逻辑删除**：所有核心业务表必须包含 `is_deleted` 字段（smallint，0未删/1已删；PostgreSQL 无 TINYINT 类型），删除操作一律执行 `UPDATE table SET is_deleted=1`，严禁物理删除 `DELETE FROM`。

3. **SQL注入防护**：
   - 在 Mapper.xml 中，`${}` 禁止用于接收前端传入的普通业务参数，**只能用于静态表名/字段名排序**（如 `order by ${sortColumn}`），且必须做白名单过滤；动态条件查询必须使用 `#{}` 预编译。

### 2.6 事务管理规范（极其重要）

ERP涉及多张表联动（如创建采购订单要同时生成库存预期入库记录、生成财务凭证），事务管理必须严谨：

- 所有 `Service` 实现类中的**增删改方法**必须加上 `@Transactional(rollbackFor = Exception.class)`。
- **事务传播机制**：默认使用 `Propagation.REQUIRED`；若需开启新事务（如日志记录），使用 `Propagation.REQUIRES_NEW`。
- **只读事务**：查询方法建议加上 `@Transactional(readOnly = true)`，有助于数据库连接优化。
- **分布式事务（微服务阶段）**：现阶段为单库本地事务，无需处理；微服务拆分后跨服务调用采用 **最终一致性** 方案（本地事务 + 消息队列 + 本地消息表/事务消息），**严禁使用强一致的XA事务** 锁死数据库。

### 2.7 金额与精度处理规范（强制）

- **数据库字段**：金额/单价/总价使用 `DECIMAL(20, 6)`（保留6位小数，防止汇率换算损失），前端展示时四舍五入为2位。
- **Java实体**：全部使用 `BigDecimal`，禁止使用 `float` / `double`。
- **计算工具类**：统一使用 `BigDecimalUtil` 工具类进行加减乘除，除法必须指定精度和舍入模式 `RoundingMode.HALF_UP`。

```java
// ❌ 错误写法
double total = price * quantity;  

// ✅ 正确写法
BigDecimal total = BigDecimalUtil.multiply(price, quantity, 2);
```

### 2.8 日志规范（强制）

1. **日志对象获取**：统一使用 Lombok 的 `@Slf4j` 注解生成 `log` 字段（Lombok 1.18.46，由 Spring Boot 4.0.8 BOM 托管版本，pom 中不写死版本号），禁止手写 `LoggerFactory.getLogger(...)`。
2. **日志级别使用**：
   - `ERROR`：系统级严重错误（数据库宕机、第三方接口不可用、空指针异常）。
   - `WARN`：业务异常（库存不足、审核不通过、参数非法）。
   - `INFO`：核心业务流程节点（订单创建成功、入库完成、定时任务启动/结束）。
   - `DEBUG`：开发调试用，生产环境关闭。

2. **审计日志（业务操作日志）**：
   - 涉及订单、工单、出入库单的 **创建、修改、审核、删除** 操作，必须调用审计日志服务，记录如下内容：
     ```
     {操作人, 操作时间, IP地址, 操作类型, 单据号, 变更前JSON, 变更后JSON}
     ```
   - 使用 AOP（切面） + 自定义注解 `@AuditLog` 实现，降低侵入性。

3. **日志打印规范**：
   - 使用 SLF4J 占位符 `{}`，禁止字符串拼接。
   - ❌ 错误：`log.info("订单创建成功：" + orderId);`
   - ✅ 正确：`log.info("订单创建成功, orderId={}, amount={}", orderId, amount);`

### 2.9 异常处理规范（统一全局拦截）

1. 自定义业务异常 `BizException`（继承 RuntimeException），携带错误码 `IErrorCode`。
2. 全局异常处理器 `GlobalExceptionHandler` 统一捕获，返回标准错误体给前端。
3. 错误码规范：
   - 200：成功
   - 400：参数错误（如必填为空、格式错误）
   - 401：未登录/Token过期
   - 403：无权限
   - 404：资源不存在
   - 500：系统内部错误
   - 10001-10999：通用错误（参数校验、权限等）
   - 11001-11999：物料/BOM 错误
   - 12001-12999：采购错误
   - 13001-13999：销售错误
   - 14001-14999：库存错误
   - 15001-15999：生产制造错误
   - 16001-16999：财务错误
   
   业务码在 `ErrorCodeEnum` 中集中定义（库存不足、订单已审核不可修改等）。

### 2.10 缓存使用规范（Redis）

- Key 命名规范：`业务模块:功能:唯一标识`，如 `inventory:stock:SKU12345`。
- 设置合理的过期时间（TTL），防止内存爆满。
- **缓存穿透/击穿/雪崩防护**：
  - 空值缓存（缓存null，过期时间短）。
  - 热点数据加锁（分布式锁 Redisson）重建缓存。
- 禁止在事务中操作缓存（先更新DB，事务提交后再更新/删除缓存）。

### 2.11 异步与定时任务规范（现阶段 @Scheduled，规模化后 XXL-JOB）

- **现阶段**使用 Spring `@Scheduled` / `@Async` 实现定时与异步任务；XXL-JOB 在规模化或微服务阶段引入，届时所有定时任务必须托管在 XXL-JOB 中，使用 `@XxlJob` 注解。
- 任务执行时必须记录开始、结束日志和耗时。
- 分布式环境下任务必须配置 **路由策略（分片广播/轮询）**，避免重复执行。
- 批量数据处理任务必须分页查询 + 批量更新，严禁一次性加载全量数据到内存。


## 三、数据库设计规范（PostgreSQL / MySQL）

### 3.1 命名规范

| 对象 | 命名规则 | 示例 |
|------|---------|------|
| 表名 | 小写字母+下划线，**单数形式**（不用复数），必须带模块前缀：`sys_`（系统）/`base_`（基础数据）/`pur_`（采购）/`sal_`（销售）/`inv_`（库存）/`mf_`（生产）/`fin_`（财务） | `pur_order`、`inv_transaction` |
| 字段名 | 小写字母+下划线 | `order_code`、`create_time` |
| 主键 | 统一使用 `id`（BIGSERIAL 自增，或应用层雪花ID传入） | `id BIGSERIAL primary key` |
| 必备字段（所有业务表必须包含） | `create_by`, `create_time`, `update_by`, `update_time`, `version`, `is_deleted` | 见下方规范 |
| 索引命名 | `idx_表名_字段名`（普通）、`uniq_表名_字段名`（唯一） | `idx_pur_order_status`、`uniq_pur_order_code` |

### 3.2 必备字段（强制所有业务表包含；单据类表另需业务单号 `order_code`）

```sql
id               BIGSERIAL       PRIMARY KEY     -- 主键（或由应用层雪花ID传入）
create_by        VARCHAR(50)     NOT NULL        -- 创建人
create_time      TIMESTAMP       NOT NULL        -- 创建时间（默认CURRENT_TIMESTAMP）
update_by        VARCHAR(50)     NOT NULL        -- 修改人
update_time      TIMESTAMP       NOT NULL        -- 修改时间（默认CURRENT_TIMESTAMP，更新时自动更新）
version          INT             DEFAULT 0       -- 乐观锁版本号
is_deleted       SMALLINT        DEFAULT 0       -- 逻辑删除标记（0:正常,1:删除；PostgreSQL 无 TINYINT）
remark           VARCHAR(500)                    -- 备注
```

### 3.3 字段类型规范

| 数据类型 | 对应Java类型 | 使用场景 |
|---------|-------------|----------|
| `BIGINT` | Long | 主键、外键、数量字段 |
| `VARCHAR(n)` | String | 编码、名称、描述等变长字符串 |
| `DECIMAL(20,6)` | BigDecimal | 金额、单价、税率 |
| `INT` | Integer | 状态码、类型码、年龄等 |
| `TIMESTAMP` | LocalDateTime | 所有时间字段 |
| `DATE` | LocalDate | 仅日期（如生日） |
| `BOOLEAN` | Boolean | 是否启用、是否审核等 |

### 3.4 索引规范

- 每个表必须建立主键索引。
- **唯一约束**：业务单号（如 `order_code`）、唯一联合键必须建立唯一索引 `UNIQUE`。
- 外键字段（如 `customer_id`、`supplier_id`）、常用于 `WHERE` 条件的状态字段（如 `status`）、时间范围查询字段（`create_time`）必须建立普通索引。
- **禁止建立过多索引**（单表索引数不超过5-6个），防止写入性能下降。


## 四、前端开发规范（Vue 3 + TypeScript）

### 4.1 目录结构

```
src/
├── api/                 // API接口定义（按模块划分）
│   ├── purchase.ts
│   ├── inventory.ts
│   └── ...
├── assets/              // 静态资源（图片、样式）
├── components/          // 公共组件（封装业务组件）
│   ├── AppButton/
│   ├── AppTable/
│   └── ...
├── composables/         // 组合式函数（hooks）
│   ├── useTable.ts
│   └── usePagination.ts
├── views/               // 页面视图（按模块划分）
│   ├── purchase/
│   │   ├── OrderList.vue
│   │   └── OrderDetail.vue
│   └── ...
├── router/              // 路由配置
├── stores/              // Pinia 状态管理（按模块）
│   ├── user.ts
│   └── app.ts
├── types/               // TypeScript 类型定义（全局）
│   ├── api.d.ts
│   └── global.d.ts
├── utils/               // 工具函数
│   ├── request.ts       // Axios封装
│   └── validate.ts
└── constants/           // 常量定义（枚举映射、下拉框配置）
```

### 4.2 编码规范

- **语言**：强制使用 TypeScript，严格模式 `strict: true`，禁止使用 `any` 类型（特殊情况必须加注释说明）。
- **UI组件库**：Ant Design Vue，优先使用其现成组件，禁止重复造轮子。
- **状态管理**：使用 Pinia，模块化定义 Store。
- **API请求**：统一使用 `axios` 封装拦截器（Token注入、统一错误提示）。

```typescript
// ✅ 正确的API定义（带类型约束）
export interface PurchaseOrder {
  id: number;
  orderCode: string;
  supplierName: string;
  totalAmount: number;
  status: OrderStatus;  // 枚举类型
}

export function getOrderList(params: QueryParams) {
  return request<PageResult<PurchaseOrder>>({
    url: '/api/v1/purchase/orders',
    method: 'get',
    params
  });
}
```

### 4.3 组件命名与规范

- **页面组件**：使用大驼峰，如 `OrderList.vue`、`SupplierManage.vue`。
- **公共组件**：加 `App` 前缀，如 `AppTable.vue`。
- **`<script setup>`**：强制使用组合式API（Composition API） + `<script setup>` 语法糖。
- Props 定义：必须使用 `defineProps` 配合 TypeScript 泛型或 `PropType`，明确定义必填、默认值。


## 五、Git 版本管理规范

### 5.1 分支模型（main + feature）

- **`main`**：生产环境分支（受保护分支），只接受人工合并，禁止直接提交，**禁止 AI 自行合并**。
- **`feature/xxx`**：功能分支，**从 `main` 拉出**，开发完成后由人工（用户）手动合并回 `main`。命名如 `feature/purchase-order-create`。
- 每个大阶段完成后由项目负责人手动 merge；不使用 develop/release/hotfix 分支。

### 5.2 Commit Message 规范（必读）

采用 **Angular规范**，格式如下：
```
<type>(<scope>): <subject>

<body>
<footer>
```

**type 类型**：
- `feat`：新功能
- `fix`：Bug修复
- `docs`：文档修改
- `style`：代码格式（不影响功能）
- `refactor`：重构（既非新功能也非修复）
- `perf`：性能优化
- `test`：测试用例
- `chore`：构建/工具变动

**示例**：
```
feat(purchase): 添加采购订单创建功能，支持多明细物料录入

- 实现订单创建Service逻辑
- 增加库存预占校验
- 补充单元测试

Closes #JIRA-1234
```

> 提交前强制检查：**禁止提交含有调试 `System.out.println` 或 `console.log` 的代码**。


## 六、代码审查（Code Review）检查清单

提交Pull Request前，开发人员必须自查；Reviewer重点检查：

| 检查项 | 说明 |
|-------|------|
| ✅ 金额是否用了BigDecimal？ | 绝对不能出现 `double`/`float` |
| ✅ 写操作是否加了事务？ | 所有增删改必须 `@Transactional` |
| ✅ 审计日志是否记录？ | 关键业务操作必须有操作日志 |
| ✅ SQL是否预编译（`#{}`）？ | 防止SQL注入 |
| ✅ 是否处理了乐观锁失败？ | 更新时处理 `version` 不一致导致的异常 |
| ✅ 缓存Key是否有TTL？ | 防止内存泄漏 |
| ✅ 异常是否抛出了而非吞掉？ | 严禁空 `catch` |
| ✅ 日志是否带占位符？ | 禁止字符串拼接 |
| ✅ API是否遵循RESTful风格？ | 统一返回格式 `Result` |
| ✅ 前端是否定义了TypeScript类型？ | 禁止 `any` |


## 七、开发环境与工具配置

| 配置项 | 推荐 | 说明 |
|-------|------|------|
| 代码格式化 | 统一使用 `.editorconfig` + 阿里巴巴Java规范插件（`Alibaba Java Coding Guidelines`） | 提交前必须格式化 |
| IDE编码 | UTF-8 | 统一字符集 |
| 换行符 | LF（Unix） | 禁止CRLF |
| Maven仓库 | 公司内部私有Nexus | 统一依赖版本管控 |
| 预提交检查 | Git Hooks + SonarLint | 提交前自动扫描脏代码 |

---
