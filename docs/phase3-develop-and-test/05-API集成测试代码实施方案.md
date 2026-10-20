# API 集成测试代码实施方案

> 阶段：Phase 3（system 模块后端完成，前端开发启动前）
> 状态：方案定稿
> 关联文档：04-API集成测试建设计划.md（策略、分级、用例清单、分步节奏由其管辖，本文档只管 Java 代码怎么写）

## 1. 形态定稿

- **框架**：`@SpringBootTest(webEnvironment = RANDOM_PORT)` + REST Assured + JUnit 5
- **环境**：不起 Docker 容器，采用**测试专用库**方案——PostgreSQL 新建 `erp_test_db`，Redis 使用独立逻辑库 db1，通过 `application-test.yml` + `@ActiveProfiles("test")` 注入
- **理由**：复用本机已安装的 PostgreSQL/Redis，零新增环境依赖；集成测试代码与容器方案共享同一接缝（连接串来源），后续迁 Testcontainers 只换配置
- **隔离级别取舍**：PG 库级隔离、Redis 逻辑库隔离（共享进程内存）；本项目 Redis 仅存短 TTL 会话数据，风险可忽略

## 2. 环境准备（一次性）

1. 创建测试库：`CREATE DATABASE erp_test_db OWNER erp_user;`
2. 调整 `erp-bootstrap/src/main/resources/application-test.yml`：数据源指向 `erp_test_db`，Redis `database: 1`
3. `erp-backend/pom.xml` 的 `<dependencyManagement>` 增加 REST Assured（test scope）
4. 根 pom surefire 增加 `it` profile：`mvn test -Pit` 仅执行 `*IntegrationTest`，默认 `mvn test` 排除之（单测不因集成测试拖慢）

启动流程不需要人工干预：Spring 上下文启动时自动执行 schema.sql 建表、AdminInitializer 幂等创建 admin。

## 3. 代码骨架设计

```
erp-backend/erp-system/src/test/java/me/north30/erp/system/
└── integration/
    ├── ApiIntegrationTest.java        # 基类：注解组合 + 登录辅助 + 端口注入
    ├── AuthIntegrationTest.java
    ├── UserIntegrationTest.java
    ├── RoleIntegrationTest.java
    ├── MenuIntegrationTest.java
    ├── DeptIntegrationTest.java
    ├── DictIntegrationTest.java
    ├── ConfigIntegrationTest.java
    ├── LogIntegrationTest.java        # 登录日志 + 审计日志
    ├── AttachmentIntegrationTest.java
    ├── ImportTaskIntegrationTest.java
    └── CodeSequenceIntegrationTest.java
```

基类职责（一个类，不做多余抽象）：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class ApiIntegrationTest {

    @LocalServerPort
    protected int port;                  // 约定一：测试类不出现任何主机名/连接串

    @Autowired
    private TestRestTemplate rest;       // 仅用于触发登录等无依赖请求

    /** 登录拿 accessToken（验证码在 test profile 放行） */
    protected String loginAsAdmin() { ... }

    /** 低权限用户登录（权限轮次用，惰性创建） */
    protected String loginAsLimited() { ... }
}
```

两条迁移铁律（保证将来 Testcontainers 化是删文件不是改代码）：

1. 测试类中禁止出现连接串、主机名、库名——一切经配置或 `@LocalServerPort` 注入
2. 不假设测试库存在既有数据——所有用例数据自建自清（容器化后测试库天然为空，零成本过渡）

## 4. 全量用例编写流程（每域 7 步）

每个业务域按同一节奏产出，以 user 域为例：

1. **读契约**：对照 API 设计文档与 Controller，列出该域全部接口路径/方法/权限点/DTO 字段
2. **列用例清单**：先写正向（CRUD 闭环）再写负向（缺必填 400、重复唯一键 422、不存在 ID 422、越权 403），负向约占 40%；对照 04 号文档 4.2 的域清单与重点
3. **写测试类**：继承 `ApiIntegrationTest`，类名 `<域>IntegrationTest`；正向用例按「创建→查询确认→修改→查询确认→删除→确认查不到」闭环展开，临时数据统一随机后缀（uuid hex 前 8 位）
4. **断言四层**：HTTP 状态码 / 响应体结构（code/message/data/timestamp/traceId）/ 业务码号段 / 关键写操作落库抽查（含 create_by 审计字段）
5. **鉴权轮次**：admin token 全通过；无 token 401、伪造 token 401、低权限 token 访问非白名单接口 403；该轮与业务轮分开写（`@Nested` 或独立方法组）
6. **执行与修复**：`mvn test -Pit -pl erp-system -am`；失败项区分三类——实现 bug（改后端）、用例预期错误（改用例）、环境问题（改配置），分开计数，禁止改预期凑绿
7. **清理验证**：连续跑两遍全绿（幂等性验证）；`erp_test_db` 残留数据按后缀抽查为零

## 5. 断言写法基线

REST Assured 统一样式（团队内不再发明第二种写法）：

```java
given()
    .port(port)
    .header("Authorization", "Bearer " + token)
    .contentType(ContentType.JSON)
    .body(new UserCreateDTO(...))
.when()
    .post("/api/system/users")
.then()
    .statusCode(200)
    .body("code", equalTo(200))
    .body("data.username", equalTo("it_" + suffix));
```

- 业务码断言直接用号段常量（`SystemErrorCode` 枚举引用），不写魔法数字
- 分页断言固定三点：`data.total`、`data.list` 非空、`data.pages` 计算
- 落库抽查用 MyBatis-Plus Mapper 直接查（测试代码允许注入 Mapper），不走 HTTP

## 6. 数据管理铁律（承接 04 号文档 4.3）

1. 临时数据一律随机后缀，重复执行不冲突
2. 每个测试方法自带清理（`@AfterEach` 按后缀兜底），清理失败必须让测试变红
3. 不触碰 admin 账号、内置角色、`is_system=1` 参数、已有菜单树种子
4. 定期归零手段：PG `DROP SCHEMA public CASCADE` 重建 + Redis `flushdb`（db1）

## 7. 运行方式

```bash
mvn test -Pit -pl erp-system -am    # system 模块集成测试（含依赖模块构建）
mvn test -Pit                        # 后续全模块
mvn test                             # 日常单测，不含集成测试
```

前置条件：本机 PostgreSQL 18.6（erp_test_db 已建）与 Redis 8 运行中。

## 8. Testcontainers 迁移预案（P4）

迁移动作清单（预计半天内完成）：

1. pom 增加 `spring-boot-testcontainers`、`org.testcontainers:postgresql` 依赖（test scope）
2. 基类增加 `@Testcontainers` + PostgreSQL/Redis 容器字段 + `@DynamicPropertySource`（覆盖 yml 连接串，优先级高于 application-test.yml）
3. 删除或保留 application-test.yml 中的连接串（保留可作无 Docker 回退）
4. 全量跑绿即迁移完成——测试类与断言零改动（由第 3 节两条铁律保证）
5. 迁移时机：接入 CI 时（04 号文档 P4 节奏）
