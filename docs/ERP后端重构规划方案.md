# ERP 后端重构规划方案

> **版本**：v1.0  
> **日期**：2026-09-01  
> **目标**：从零构建 Maven 多模块 Spring Boot 单体项目，后续平滑过渡到微服务

---

## 一、架构定位

| 维度 | 当前阶段 | 未来方向 |
|------|----------|----------|
| 架构模式 | **模块化单体（Modular Monolith）** | 微服务架构（Spring Cloud） |
| 部署方式 | 单一 JAR 包部署 | 按模块独立部署 |
| 模块间调用 | Java 方法调用（Service 接口） | HTTP 远程调用（Feign） |
| 模块数量 | **5 个模块** | 按需拆分 |

---

## 二、后端目录结构

```
erp-backend/                          # 后端根目录
├── pom.xml                           # 父 POM（统一版本管理）
│                                     # packaging = pom
│
├── erp-common/                       # 公共模块（纯工具，不依赖任何业务模块）
│   ├── pom.xml
│   └── src/main/java/me/north30/erp/common/
│       ├── Result.java               # 统一响应体 {code, message, data}
│       ├── BusinessException.java    # 业务异常（code + message）
│       ├── GlobalExceptionHandler.java # @RestControllerAdvice 全局拦截
│       └── JwtTokenProvider.java    # JWT 生成与解析（基于 NimbusJwtEncoder/Decoder）
│
├── erp-system/                       # 系统管理模块（用户/角色/权限/菜单）
│   ├── pom.xml
│   └── src/main/java/me/north30/erp/system/
│       ├── controller/               # AuthController（登录）、UserController
│       ├── service/                  # AuthService、UserService
│       ├── mapper/                   # SysUserMapper、SysRoleMapper
│       ├── entity/                   # SysUser、SysRole、SysMenu
│       ├── dto/                      # LoginDTO（登录入参）
│       └── vo/                       # LoginVO（登录出参）
│
├── erp-base/                         # 基础数据模块（物料/BOM/客商/仓库）
│   ├── pom.xml
│   └── src/main/java/me/north30/erp/base/
│       ├── controller/               # MaterialController、BomController 等
│       ├── service/                  # MaterialService、BomService 等
│       ├── mapper/                   # MaterialMapper、BomMapper 等
│       ├── entity/                   # Material、Bom、BomItem、Customer 等
│       └── dto/                      # MaterialDTO、BomDTO 等
│
├── erp-business/                     # 业务模块（采购/销售/库存/生产/财务）
│   ├── pom.xml                       # 依赖 erp-common + erp-system + erp-base
│   └── src/main/java/me/north30/erp/business/
│       ├── purchase/                 # 采购管理（采购申请→订单→收货→入库）
│       │   ├── controller/
│       │   ├── service/
│       │   ├── mapper/
│       │   ├── entity/
│       │   └── dto/
│       ├── sales/                    # 销售管理（销售订单→发货→出库→开票）
│       │   └── ...
│       ├── inventory/                # 库存管理（入库/出库/调拨/盘点/流水）
│       │   └── ...
│       ├── manufacturing/            # 生产管理（MRP→工单→领料→报工→入库）
│       │   └── ...
│       └── finance/                  # 财务管理（应收/应付/凭证/成本核算）
│           └── ...
│
└── erp-bootstrap/                    # 启动模块（唯一入口，不写业务代码）
    ├── pom.xml                       # 依赖所有其他模块
    └── src/main/java/me/north30/erp/
        ├── ErpApplication.java       # @SpringBootApplication 启动类
        ├── config/
        │   ├── SecurityConfig.java   # Spring Security 配置
        │   └── MyBatisPlusConfig.java # MyBatis-Plus 分页/自动填充配置
        └── resources/
            ├── application.yml       # 数据源/Redis/JWT 配置
            └── mapper/               # MyBatis XML（按模块分文件夹）
                ├── system/
                ├── base/
                └── business/
                    ├── purchase/
                    ├── sales/
                    ├── inventory/
                    ├── manufacturing/
                    └── finance/
```

---

## 三、模块依赖关系图

```
                        ┌──────────────────────┐
                        │    erp-bootstrap      │  ← 启动入口，依赖所有模块
                        │   (唯一启动入口)       │
                        └──────┬───────────────┘
                               │ depends on
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
   ┌─────────────┐    ┌──────────────┐    ┌──────────────┐
   │  erp-system  │    │   erp-base   │    │ erp-business  │
   │ (系统管理)    │    │ (基础数据)    │    │  (业务模块)    │
   └──────┬──────┘    └──────┬───────┘    └──────┬───────┘
          │                  │                    │
          └────────────┬─────┘                    │
                       │                          │
                       ▼                          ▼
                ┌──────────────────────────────────────┐
                │            erp-common                 │
                │   (统一响应/异常处理/JWT工具/公共Util)  │
                └──────────────────────────────────────┘
```

**依赖规则**：
- `erp-common` → 不依赖任何模块，是所有模块的基础
- `erp-system` → 依赖 `erp-common`
- `erp-base` → 依赖 `erp-common`
- `erp-business` → 依赖 `erp-common` + `erp-system` + `erp-base`
- `erp-bootstrap` → 依赖所有模块，**唯一包含 `spring-boot-maven-plugin` 的模块**

---

## 四、开发阶段规划

### 第一阶段：骨架搭建（1-2 周）

| 步骤 | 内容 | 产出 |
|------|------|------|
| 1.1 | 创建父 POM + 5 个子模块 POM | 项目骨架可编译 |
| 1.2 | 编写 `erp-common`：Result、BusinessException、GlobalExceptionHandler、JwtUtil | 公共模块完成 |
| 1.3 | 编写 `erp-bootstrap`：启动类、SecurityConfig、application.yml | 项目可启动 |
| 1.4 | 建表：sys_user、sys_role、sys_user_role | 数据库就绪 |
| 1.5 | 编写 `erp-system`：登录接口（AuthController + AuthService） | 登录接口可调通 |

**里程碑**：`curl -X POST /api/v1/auth/login` 返回 token ✅

### 第二阶段：系统管理完善（2-3 周）

| 步骤 | 内容 |
|------|------|
| 2.1 | 用户管理 CRUD（分页查询/新增/编辑/启用禁用） |
| 2.2 | 角色管理 CRUD + 用户角色分配 |
| 2.3 | 菜单管理 + 权限管理 |
| 2.4 | JWT 拦截器（校验 token、解析用户信息） |
| 2.5 | 审计日志（操作记录自动记录） |

### 第三阶段：基础数据（3-4 周）

| 步骤 | 内容 |
|------|------|
| 3.1 | 物料管理（物料编码、分类、规格参数） |
| 3.2 | BOM 管理（BOM 头 + BOM 行，支持多级 BOM） |
| 3.3 | 客户/供应商管理（客商档案） |
| 3.4 | 仓库/库位管理 |

### 第四阶段：核心业务链（6-8 周）

| 步骤 | 内容 |
|------|------|
| 4.1 | 销售管理：销售订单 → 发货通知 → 出库单 |
| 4.2 | 采购管理：采购申请 → 采购订单 → 收货单 → 质检 → 入库单 |
| 4.3 | 库存管理：库存流水（Transaction）、可用量计算、盘点、调拨 |
| 4.4 | 生产管理：MRP 运算 → 生产工单 → 领料 → 报工 → 完工入库 |

### 第五阶段：财务集成（4-6 周）

| 步骤 | 内容 |
|------|------|
| 5.1 | 应收应付管理 |
| 5.2 | 存货核算（移动加权平均） |
| 5.3 | 总账凭证自动生成（业财一体化） |
| 5.4 | 成本核算（按工单归集） |

### 第六阶段：报表与优化（持续）

| 步骤 | 内容 |
|------|------|
| 6.1 | 经营仪表盘 |
| 6.2 | 各业务模块报表 |
| 6.3 | 权限精细化 |
| 6.4 | 性能优化 + 缓存策略 |

---

## 五、技术栈

| 层级 | 选型 | 版本 |
|------|------|------|
| 语言 | Java | 21 LTS |
| 框架 | Spring Boot（模块化单体） | 4.0.x（4.0.8） |
| 安全 | Spring Security | 7.x |
| ORM | MyBatis-Plus | 3.5.17（mybatis-plus-spring-boot4-starter + mybatis-plus-jsqlparser） |
| 数据库 | PostgreSQL | 16+ |
| 缓存 | Redis | 8.x |
| 认证 | spring-boot-starter-oauth2-resource-server | NimbusJwtEncoder / NimbusJwtDecoder |
| API 文档 | springdoc-openapi | 3.x（Knife4j 4.x 不兼容 Boot 4，暂不采用） |
| 构建 | Maven | 3.9+ |
| 密码加密 | BCrypt（Spring Security） | - |

---

## 六、核心设计原则

### 6.1 代码规范
- 每个模块内部按 `controller → service → mapper → entity → dto/vo` 分层
- 不允许跨模块直接调用 Mapper，必须通过 Service 接口
- 所有接口返回统一格式 `Result<T>`，业务异常抛 `BusinessException`
- 错误码区间：10001-10999 通用、11001-11999 物料/BOM、12001-12999 采购、13001-13999 销售、14001-14999 库存、15001-15999 生产、16001-16999 财务

### 6.2 数据库规范
- 表名统一前缀：`sys_`（系统）、`base_`（基础数据）、`pur_`（采购）、`sal_`（销售）、`inv_`（库存）、`mf_`（生产）、`fin_`（财务）
- 每张表必须有 `id`（BIGSERIAL 主键）、`create_by`、`create_time`、`update_by`、`update_time`（审计字段）、`version`（乐观锁）、`is_deleted`（逻辑删除）、`remark`
- 关键业务表（如库存）使用乐观锁（`version` 字段）

### 6.3 库存设计核心原则
- 任何库存变动必须通过**库存流水表（inv_transaction）**记录
- 先写流水，再更新库存余额
- 库存更新使用乐观锁防止并发超卖

---

## 七、后续过渡到微服务的路径

| 当前（模块化单体） | 未来（微服务） |
|------------------|---------------|
| `erp-business` 里的包 | 提取为独立模块 |
| Service 方法调用 | 改为 Feign HTTP 调用 |
| 单一数据库 | 按服务拆分数据库 |
| 单一 JAR 部署 | 独立部署 + Nacos 注册 |

**过渡时机**：当某个业务模块的代码量超过 5000 行，或需要独立扩缩容时，再考虑拆分。

---

## 八、启动方式

```bash
# 进入后端项目
cd ~/projects/Personal/north30-erp-project/erp-backend

# 编译所有模块
mvn clean compile

# 启动（仅启动 bootstrap 模块，其他模块作为依赖引入）
mvn spring-boot:run -pl erp-bootstrap

# 打包
mvn clean package -DskipTests
# 产物：erp-bootstrap/target/erp-bootstrap-1.0.0-SNAPSHOT.jar
```
