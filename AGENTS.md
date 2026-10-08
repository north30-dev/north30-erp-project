# AGENTS.md — AI 编码代理工作规则

本文件是 AI 代理（Agent）在本项目工作的强制规则。任何规则冲突时，以 `docs/` 下的正式文档为准。

## 1. 项目定位

- north30 ERP：面向 MTO 离散制造企业的 ERP 系统，**模块化单体**架构（非微服务），后续可平滑拆分。
- 技术栈：Java 21 + Spring Boot 4.0.8 + Spring Security 7 + MyBatis-Plus 3.5.17 + PostgreSQL 16 + Redis 8；前端 Vue 3.4+ / TypeScript / Ant Design Vue 4 / Pinia / Vite。
- 回复语言与代码注释语言遵循用户当前消息语言。

## 2. 权威文档索引（docs/）

| 文档 | 管辖范围 |
|------|---------|
| 企业资源计划ERP系统需求规格说明书.md | 功能范围、业务场景、验收标准 |
| ERP系统开发技术选型.md | 技术栈与版本基线 |
| ERP系统开发目录结构设计.md | 工程结构 |
| ERP后端重构规划方案.md | 后端模块划分、依赖规则、启动方式 |
| ERP系统开发API设计与规范文档.md | 接口设计、统一响应体、错误码 |
| ERP系统数据库表结构设计文档.md | 表结构、必备字段、索引 |
| ERP系统开发编码规范文档.md | 编码铁律、分层约束、Git 规范 |
| ERP系统开发阶段计划文档.md | 7 阶段计划、D1-D9 子阶段、里程碑 M1-M10 |

动手写代码前，先读对应管辖文档；接口与表结构**必须**以 API 文档和数据库设计文档为准。

## 3. 架构约束

- 5 个 Maven 模块：`erp-common` / `erp-system` / `erp-base` / `erp-business`（内部按 purchase / sales / inventory / manufacturing / finance 分包）/ `erp-bootstrap`。
- 依赖规则：common 不依赖任何模块；system、base 仅依赖 common；business 依赖 common + system + base；bootstrap 依赖所有模块，是**唯一启动入口**（唯一含 spring-boot-maven-plugin）。
- **跨模块禁止直接调用 Mapper，必须通过 Service 接口**。
- `erp-bootstrap` 不写业务代码，只放启动类、配置类和 application.yml。

## 4. 编码铁律（不可违反）

1. **金额计算必须 BigDecimal**：单价/金额/税率/折扣禁用 float/double，统一走 `BigDecimalUtil`，除法指定 `RoundingMode.HALF_UP`；数据库金额字段 `DECIMAL(20,6)`。
2. **写操作必须显式事务**：Service 增删改方法必须 `@Transactional(rollbackFor = Exception.class)`；查询建议 `@Transactional(readOnly = true)`。
3. **审计日志**：订单/工单/出入库单的创建、修改、审核、删除必须通过 `@AuditLog` 注解（AOP）记录操作人、时间、IP、操作类型、单据号、变更前后 JSON。
4. **逻辑删除**：核心业务表带 `is_deleted`（SMALLINT，PostgreSQL 无 TINYINT），禁止物理 `DELETE FROM`。
5. **统一响应**：所有接口返回 `Result<T>`（me.north30.erp.common.result.Result）；业务校验失败直接抛 `BusinessException`，由 `GlobalExceptionHandler` 统一处理，严禁 try-catch 吞异常。
6. **错误码区间**：200 成功；10001-10999 通用；11001-11999 物料/BOM；12001-12999 采购；13001-13999 销售；14001-14999 库存；15001-15999 生产；16001-16999 财务。集中定义在 ErrorCode 枚举。
7. **数据库访问**：单表查询用 `LambdaQueryWrapper`；多表关联/报表写 Mapper.xml 并注释；动态条件用 `#{}` 预编译（`${}` 仅限静态表名/排序字段且需白名单）；**禁止循环内查库（N+1）**。
8. **库存核心原则**：任何库存变动必须先写 `inv_transaction` 流水，再更新余额；更新带乐观锁 `version`，必须处理乐观锁失败。
9. **分层职责**：Controller 只做参数校验/调 Service/封装响应，禁止业务逻辑和直接返回 Entity（DTO 入、VO 出）；Entity 贫血模型；DTO 禁引用 Entity。
10. **禁止调试输出**：`System.out.println` / `console.log` 不允许出现在提交代码中；日志用 SLF4J 占位符 `{}`。

## 5. 包结构与命名

- 分层与业务域分包：`me.north30.erp.<模块>.<业务域>.{controller, service, service.impl, mapper, entity, dto, vo}`，第一级按业务域分包（不设 core 中间层）；模块内跨域公共部分放 `<模块>.common.{util, enums, vo}`，安全上下文放 `<模块>.security`。
- 业务域划分：system 模块按 auth/user/role/menu/dept/dict/config/log/attachment/importtask/codesequence 分域；business 模块按 purchase/sales/inventory/manufacturing/finance 分域；base 模块按物料/BOM/工厂等基础数据域分包。
- 小 DDD（编码规范 2.13）：Entity 局部充血（只校验自身状态 + 修改自身字段 + 抛 BusinessException）；可命名复用逻辑下沉 `<domain>/strategy` 策略类（Query/Assemble/Grant/IO 类）；ServiceImpl 只留编排，禁止私有方法仓库；**跨域禁止直接注入 Mapper**，须经目标域 Service 的 requireXxx/countByXxx/listByXxx 等最小接口。
- 命名：Entity 对应表名大驼峰（`PurOrder` ↔ `pur_order`）；DTO/VO 后缀 `DTO`/`VO`；实现类 `Impl` 后缀；枚举 `Enum` 后缀；工具类 `Util` 后缀。
- DTO/VO 一律 record（含 GET 查询对象），树形结构 children 用自底向上递归构造；record 类 Javadoc 必须 `@param` 逐字段注释。
- GET 查询对象分页参数统一 `@Min(1)`/`@Max(200)` + Controller `@Valid` 入口校验（400）；Service normalize 仅做缺省兜底与防御（超限 422）。
- 实体继承 `BaseEntity`（审计字段 id/create_by/create_time/update_by/update_time/version/is_deleted/remark 自动填充，勿重复定义）。
- 表名前缀：`sys_`（系统）/ `base_`（基础数据）/ `pur_`（采购）/ `sal_`（销售）/ `inv_`（库存）/ `mf_`（生产）/ `fin_`（财务），单数形式。
- API 前缀统一 `/api`，RESTful 风格，动词场景走 POST（如 `/api/purchase/orders/{id}/approve`）。

## 6. 技术栈红线

- **Lombok 1.18.46**：由 Boot 4.0.8 BOM 托管版本（pom 中不写死版本号），统一用 `@Slf4j` 生成日志对象（禁止手写 `LoggerFactory.getLogger`）、`@Data`/`@Getter`/`@Setter` 简化 Entity/DTO/VO 样板代码。
- JWT：用 `spring-boot-starter-oauth2-resource-server` + NimbusJwtEncoder/Decoder，**禁用 jjwt**；secret/TTL 配置在 application.yml 的 `erp.jwt` 下。
- MyBatis-Plus：用 `mybatis-plus-spring-boot4-starter`，**必须额外引入 `mybatis-plus-jsqlparser`**（3.5.9+ 分页插件拆分）。
- API 文档：springdoc-openapi 3.x，**Knife4j 4.x 不兼容 Boot 4，禁用**。
- Boot 4 配置迁移：`server.servlet.encoding` → `spring.servlet.encoding`。
- Redis Key 命名：`业务模块:功能:唯一标识`（如 `inventory:stock:SKU12345`），必须设 TTL；先更新 DB，事务提交后再操作缓存。
- 定时任务现阶段用 Spring `@Scheduled`/`@Async`；XXL-JOB 到规模化/微服务阶段再引入。
- 前端：TS 严格模式禁 `any`（特殊必须注释）；强制 `<script setup>` 组合式 API；页面组件大驼峰、公共组件 `App` 前缀；axios 统一封装（Token 注入、统一错误提示）。

## 7. Git 规则（对 AI 的硬约束）

- 分支模型：`main` + `feature/xxx`，**不使用 develop/release/hotfix**。
- `main` 受保护：**AI 禁止直接向 main 提交，禁止自行执行 merge**；功能开发在 `feature/xxx` 分支（从 main 拉出）。
- 功能完成后由**用户手动**合并回 main。
- Commit Message 用 Angular 规范：`<type>(<scope>): <subject>`，type ∈ feat/fix/docs/style/refactor/perf/test/chore。
- 未经用户明确要求，AI 不主动 commit / push。

## 8. 开发环境

| 组件 | 说明 |
|------|------|
| PostgreSQL 18.6 | 本机安装（原 Docker 容器 `erp-postgres` 已停止），端口 5432，用户 `erp_user`，密码 `erp_password`，库 `erp_db` |
| Redis 8 | 本机运行，密码 `root` |
| JDK / Maven | Java 21 LTS / Maven 3.9+ |
| admin 初始密码 | `Admin@123456`（AdminInitializer 幂等创建） |

后端启动方式：

```bash
cd erp-backend
mvn clean compile
mvn spring-boot:run -pl erp-bootstrap
```

## 9. 已知陷阱

- 编辑文件遵守 `.editorconfig`：UTF-8、LF 换行（禁止 CRLF）、Java 4 空格缩进、前端/JSON/YAML 2 空格缩进。

## 10. 当前进度

- 旧代码已全部删除，项目按 docs/ 文档从零重写；git 仓库已重建（main 分支，首个提交由用户手动完成）。
- 开发顺序（D1-D9）：骨架 → 系统管理 → 基础数据 → 库存 → 采购 → 销售 → 生产 → 财务 → 报表；每个子阶段后端与前端同步交付，退出标准：功能可联调 + 单测通过 + 代码审查完成 + API 文档与数据库脚本同步更新。
