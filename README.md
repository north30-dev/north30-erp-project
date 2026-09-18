# north30 ERP 系统

面向**按订单生产（MTO）离散制造企业**的 ERP 系统，覆盖「销售接单 → MRP 运算 → 采购/生产 → 出入库 → 开票收款 → 财务凭证」全链路，实现业务财务一体化、供应链全程可视化、库存精准管控与产品级成本核算。

## 功能模块

| 序号 | 模块 | 说明 |
|------|------|------|
| 0 | 系统管理 | 用户/角色/权限/菜单、认证鉴权（JWT）、审计日志 |
| 1 | 基础数据管理 | 物料、BOM、供应商、客户、组织架构、仓库/库位 |
| 2 | 销售管理 | 销售订单 → 发货单 → 出库 → 开票 → 应收账款 |
| 3 | 采购管理 | 采购申请 → 订单 → 收货 → 质检 → 入库 → 应付账款 |
| 4 | 库存管理 | 多仓库/库位、库存流水、批次追溯、盘点、调拨、安全库存预警 |
| 5 | 生产制造管理 | MRP 运算、生产工单、领料/报工/完工入库 |
| 6 | 财务管理 | 应收应付、存货核算（移动加权平均）、总账凭证自动生成、成本核算 |
| 7 | 报表与分析 | 经营仪表盘、销售/采购/库存/生产分析报表 |

## 技术栈

> **Java 21 + Spring Boot 4.0.x（模块化单体）+ MyBatis-Plus 3.5.17 + Vue 3 + TypeScript + Ant Design Vue + PostgreSQL 16 + Redis 8 + springdoc-openapi 3**

| 层级 | 选型 | 版本 |
|------|------|------|
| 语言 | Java | JDK 21 LTS |
| 后端框架 | Spring Boot（模块化单体） | 4.0.x（4.0.8），Spring Cloud 2025.x 为微服务阶段演进 |
| 安全 | Spring Security + oauth2-resource-server | 7.x（NimbusJwtEncoder/Decoder） |
| 代码简化 | Lombok | 1.18.46（Boot 4.0.8 BOM 托管，@Slf4j/@Data 等） |
| ORM | MyBatis-Plus | 3.5.17（mybatis-plus-spring-boot4-starter + mybatis-plus-jsqlparser） |
| 数据库 | PostgreSQL | 16+（Docker 部署） |
| 缓存 | Redis | 8.x |
| API 文档 | springdoc-openapi | 3.x |
| 前端框架 | Vue 3 + TypeScript + Ant Design Vue | Vue 3.4+ / AntD Vue 4.x |
| 状态管理 | Pinia | 4.x |
| 构建 | Maven / Vite | Maven 3.9+ / Vite 8 |

现阶段为**模块化单体**：单一 Spring Boot 应用 + 单一 JAR 部署；RabbitMQ、Nacos、Sentinel、XXL-JOB、Spring Cloud Gateway、K8s 等组件在微服务拆分阶段按需引入。

## 系统架构

5 个 Maven 模块，模块间通过 Service 接口调用（禁止跨模块直接调用 Mapper）：

```
                    ┌──────────────────────┐
                    │    erp-bootstrap      │  ← 唯一启动入口（唯一含 spring-boot-maven-plugin）
                    └──────┬───────────────┘
          ┌────────────────┼────────────────┐
          ▼                ▼                ▼
   ┌─────────────┐  ┌─────────────┐  ┌──────────────┐
   │  erp-system  │  │   erp-base   │  │ erp-business  │
   │  (系统管理)   │  │  (基础数据)   │  │ (采购/销售/库存 │
   └──────┬──────┘  └──────┬───────┘  │ /生产/财务)    │
          │                │          └──────┬───────┘
          └────────┬───────┘                 │
                   ▼                         │
          ┌──────────────────────────────────▼─┐
          │            erp-common               │
          │  (Result统一响应/异常处理/JWT工具/Util) │
          └────────────────────────────────────┘
```

依赖规则：`erp-common` 不依赖任何模块；`erp-system`、`erp-base` 依赖 `erp-common`；`erp-business` 依赖 `erp-common + erp-system + erp-base`；`erp-bootstrap` 依赖所有模块。

## 项目结构

```
north30-erp-project/
├── README.md / AGENTS.md            # 项目说明 / AI 代理工作规则
├── .gitignore / .editorconfig       # UTF-8 + LF，Java 4 空格、前端 2 空格
├── docs/                            # 项目文档（8 篇，见下方文档索引）
├── erp-backend/                     # 后端（Maven 5 模块：common/system/base/business/bootstrap）
├── erp-frontend/                    # 前端（Vite + Vue3 + TS + AntD Vue 4 + Pinia）
├── database/                        # 数据库脚本（init / migration，规划中）
├── deploy/                          # 部署配置
└── scripts/                         # 辅助脚本
```

## 开发计划概览

全生命周期 7 阶段（12 个月）：需求确认 → 系统设计 → 开发与单元测试（D1-D9）→ SIT → UAT → 数据迁移与上线 → 上线支持与终验。

开发期按依赖链推进，每个子阶段后端与前端同步交付：

| 子阶段 | 内容 | 里程碑 |
|--------|------|--------|
| D1 骨架搭建 | 父 POM 与模块划分；统一响应/全局异常/错误码/审计日志 AOP | 工程可编译可启动 |
| D2 系统管理 | 认证与 JWT；用户/角色/权限；审计日志 | M3 登录返回 token |
| D3 基础数据 | 物料/分类；BOM（含闭环检测）；客商档案；仓库/库位 | M4 主数据可用 |
| D4 库存 | 库存流水；批次；盘点；调拨；安全库存预警 | 库存变动全可追溯 |
| D5 采购 | 申请→订单→收货→质检→入库；三单匹配；采购退货 | 采购全链路可演示 |
| D6 销售 | 订单→发货→出库；销售退货；信用控制 | 销售全链路可演示 |
| D7 生产 | MRP；生产工单；领料；报工；完工入库 | M5 核心业务链完成 |
| D8 财务 | 应收应付；存货核算；凭证自动生成；成本归集 | M6 业财一体完成 |
| D9 报表与优化 | 经营仪表盘；业务报表；权限精细化；性能优化 | M7 Feature Complete |

## 文档索引

| 文档 | 用途 |
|------|------|
| [企业资源计划ERP系统需求规格说明书](docs/企业资源计划ERP系统需求规格说明书.md) | 功能范围、业务场景、验收标准 |
| [ERP系统开发技术选型](docs/ERP系统开发技术选型.md) | 技术栈版本基线与选型理由 |
| [ERP系统开发目录结构设计](docs/ERP系统开发目录结构设计.md) | 模块划分与工程结构 |
| [ERP后端重构规划方案](docs/ERP后端重构规划方案.md) | 后端模块依赖、开发阶段、启动方式 |
| [ERP系统开发API设计与规范文档](docs/ERP系统开发API设计与规范文档.md) | RESTful 接口设计、统一响应体、错误码 |
| [ERP系统数据库表结构设计文档](docs/ERP系统数据库表结构设计文档.md) | 全量表结构、必备字段、索引规范 |
| [ERP系统开发编码规范文档](docs/ERP系统开发编码规范文档.md) | 编码铁律、分层约束、Git 规范 |
| [ERP系统开发阶段计划文档](docs/ERP系统开发阶段计划文档.md) | 7 阶段计划、里程碑 M1-M10、质量闸门 |

## Git 工作流

- 分支模型：`main` + `feature/xxx`（从 main 拉出，如 `feature/purchase-order-create`），不使用 develop/release/hotfix。
- `main` 为受保护分支，只接受人工合并；开发完成后由项目负责人手动 merge。
- Commit Message 采用 Angular 规范：`feat / fix / docs / style / refactor / perf / test / chore`。
- 提交前禁止包含调试输出（`System.out.println` / `console.log`）。

## 开发环境

| 组件 | 说明 |
|------|------|
| JDK | 21 LTS |
| Maven | 3.9+ |
| PostgreSQL 16 | Docker 容器 `erp-postgres`，端口 5432，用户/库 `erp`，密码 `erp@123456` |
| Redis 8 | 本机运行，密码 `root` |
| Node.js | 前端构建（Vite 8） |

后端启动（工程创建后）：

```bash
cd erp-backend
mvn clean compile
mvn spring-boot:run -pl erp-bootstrap
```

编码约定速览：统一 UTF-8 + LF 换行符；统一响应体 `Result<T>`；金额计算必须 `BigDecimal`；增删改必须 `@Transactional(rollbackFor = Exception.class)`；关键写操作记录审计日志；API 前缀 `/api`。完整规范见 [AGENTS.md](AGENTS.md) 与 [编码规范文档](docs/ERP系统开发编码规范文档.md)。
