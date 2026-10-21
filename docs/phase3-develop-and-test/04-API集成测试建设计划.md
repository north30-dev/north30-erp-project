# API 集成测试建设计划

> 阶段：Phase 3（system 模块已进入联调稳定期）
> 状态：方案定稿，分步实施
> 关联文档：阶段计划文档（D1-D9）、API 设计与规范文档、编码规范文档

## 1. 目标与定位

建立**黑盒 API 集成测试**体系：对运行中的后端发起真实 HTTP 请求，验证 Controller → Service → Mapper → PostgreSQL/Redis 全链路的正确性，重点覆盖"接缝"问题——单元测试（Mockito）测不到的部分：

- Security 配置与 Controller 权限点的一致性（@PreAuthorize 与 sys_menu 种子对齐）
- 入口校验（DTO 注解 + @Valid）与真实 HTTP 行为的一致性（400/422 的边界）
- 业务码号段与错误场景的映射（GlobalExceptionHandler 统一响应）
- 乐观锁、唯一性冲突、逻辑删除等持久层行为
- @AuditLog 切面、@Transactional 回滚等横切行为

### 与现有测试体系的关系

| 层级 | 覆盖物 | 现状 |
|------|--------|------|
| 单元测试 | ServiceImpl/策略类/Entity 充血方法（全 Mock） | 385 个，持续维护 |
| **API 集成测试（本计划）** | HTTP 接口 + 真实 DB/Redis | system 模块已人工验证一轮，待体系化 |
| E2E | 前端页面操作 | 前端未开发，暂缓 |

## 2. 测试策略

### 2.1 用例分级

| 级别 | 范围 | 数据策略 |
|------|------|---------|
| **只读用例** | 全部 GET 接口 | 零风险，含分页边界（pageSize=0→400、>200→400）、不存在 ID→422 |
| **写用例** | POST/PUT/DELETE | 走"创建→查询确认→修改→删除→确认查不到"闭环，临时数据带随机后缀，可重复执行 |
| **危险操作** | 重置 admin 密码、删 admin 角色、改系统内置参数、真实附件上传 | 默认跳过或仅测负向（非法参数 422），不真改环境 |

### 2.2 断言维度（每用例四层）

1. HTTP 状态码：200/400/401/403/404/422 与设计一致
2. 统一响应体结构：code/message/data/timestamp/traceId 齐全
3. 业务码：落在正确号段（10001 通用 / 18001-18999 system / 12001+ 采购…）
4. 数据落库抽查：关键写操作用 SQL 验证 sys_xxx 表实际数据（含 create_by 等审计字段）

### 2.3 鉴权维度（独立一轮）

- 无 token → 401/10002；伪造 token → 401/10002；有效 token 访问不存在路径 → 404
- **低权限用户 token**：新建无权限角色+用户，断言非白名单接口全部 403/10003（防止权限点缺失类缺陷复发）
- token 过期/refresh 换发/logout 失效

## 3. 已验证的基线（system 模块）

2026-10-15 人工执行过一轮全量接口验证，60 个用例全绿，发现的缺陷均已修复：

| 缺陷 | 修复 |
|------|------|
| 菜单种子缺 25 个权限点（dept/dict/config CRUD、user detail/export 等） | AdminInitializer 补齐 + ensureTree 幂等同步 perms |
| 方法级 403 落兜底变 500 | GlobalExceptionHandler 增加 AccessDeniedException → 403/10003 |
| 无效 token 401 无响应体 | oauth2ResourceServer DSL 挂 authenticationEntryPoint |
| 请求体缺失/JSON 非法返回 500 | GlobalExceptionHandler 增加 HttpMessageNotReadableException → 400/10001 |
| configKey 无格式校验 | ConfigCreateDTO @Pattern 小写域前缀.参数名 |

已知行为基线（写用例时的预期依据）：

- 权限集合在登录时固化进 token，改权限后须重新登录
- 入口校验（@Valid）失败返回 400/10001；业务规则违反返回 422 + 域内业务码
- 附件 list 必须带 bizType+bizId（按业务主键查询）
- 编号序列重置 DTO 为 {bizType, period, currentNo, reason}

## 4. 实施方案

### 4.1 形态选型

**P1 起采用 Java 集成测试代码**：`@SpringBootTest(RANDOM_PORT)` + REST Assured + JUnit 5，实施细节见 `05-API集成测试代码实施方案.md`。

- 环境采用**测试专用库**（PG 独立库 `erp_test_db` + Redis 逻辑库 db1），`@ActiveProfiles("test")` + application-test.yml 注入，复用本机 PG/Redis 零新增依赖
- 通过测试类不写连接串、不依赖既有数据两条铁律，保证后续迁 Testcontainers 只换配置来源不改测试代码

中期（接入 CI 时）演进为 **Testcontainers**：

- PostgreSQL 16 + Redis 8 临时容器，CI 可自动跑，不依赖本地环境
- 前置条件：开发机需恢复 Docker 环境

### 4.2 用例清单规划（按域）

| 域 | 用例规模 | 重点 |
|----|---------|------|
| auth | ~10 | 登录/验证码/刷新/登出/改密，token 生命周期 |
| user | ~12 | CRUD 闭环、改状态、重置密码、分配角色、导出、乐观锁 |
| role | ~10 | CRUD、数据范围、菜单分配、内置角色保护 |
| menu | ~6 | 树、增删（不动既有种子）、类型与 path/component/perms 配套校验 |
| dept | ~8 | CRUD 闭环、父子层级约束（有子部门禁删） |
| dict | ~10 | 类型+项两级、启停联动 |
| config | ~8 | CRUD、key 格式、内置参数保护 |
| log（登录/审计） | ~5 | 分页、时间范围过滤 |
| attachment | ~6 | 上传/下载/删除闭环、大小与数量上限（走 sys_config 配置） |
| importtask | ~3 | 空占位行为 |
| codesequence | ~4 | 列表、重置、不存在 422 |

负向用例约占 40%，每个域必含：缺必填 400、重复唯一键 422、不存在 ID 422、越权 403。

### 4.3 数据管理铁律

1. 临时数据一律带随机后缀（uuid hex 前 8 位），保证重复执行
2. 每个写用例域结束时清理本次创建的数据；清理失败要在报告中显式暴露
3. 断言用的业务 key 命名必须符合后端校验规则（如 configKey 每段字母开头）
4. 不触碰 admin 账号、内置角色、is_system=1 的参数、已有菜单树

### 4.4 分步节奏（对齐 D1-D9）

| 步骤 | 内容 | 退出标准 |
|------|------|---------|
| **P0（已完成）** | system 模块全接口人工验证，缺陷清零 | 60 用例全绿 |
| P1 | 按 05 号文档搭建集成测试骨架（基类 + it profile + 测试专用库），system 模块 11 域 Java 集成测试成套编写，补低权限 token 轮次 | `mvn test -Pit` 全绿且可重复执行 |
| P2（随 D3 基础数据） | base 模块（物料/BOM/工厂）用例，随域交付同步补充 | 每交付一域即有一域用例 |
| P3（随 D4-D8 业务模块） | 库存/采购/销售/生产/财务，重点是**单据流转链**（如采购单建→审→收货→入库→核销）与库存流水/乐观锁断言 | 每域 3-5 个链路用例 |
| P4 | JUnit + Testcontainers 迁移评估，接入 CI（mvn verify） | CI 全自动回归 |

## 5. 报告与追踪

- 每轮执行输出：总数/通过/失败/耗时 + 失败用例的请求 URL、请求体、实际响应
- 完整结果落 JSON 工件（不入库），失败项附根因判定：实现 bug / 用例预期错误 / 环境问题
- 用例预期写错与实现 bug 分开计数，避免"改预期凑绿"掩盖真实缺陷

## 6. 风险与约束

- 集成测试打真库，严禁在无清理策略的情况下批量造数（用户/角色/部门膨胀）
- 单据流转链用例（P3）依赖业务域状态机设计稳定，过早编写会随接口调整返工
- Testcontainers 阶段需 Docker；本机当前为原生安装的 PG/Redis，迁移时需评估 CI 资源
- 附件上传用例依赖前端直传模式之外的程序化 multipart 调用，注意与 sys_config 的附件上限配置联动
