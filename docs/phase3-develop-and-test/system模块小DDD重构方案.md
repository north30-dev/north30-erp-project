# system 模块小 DDD 重构方案

> **状态：已完成（2026-10-08）**。system 模块 10 个域全部按本方案重构完毕，全量单测 338 个通过；规范已沉淀至编码规范文档 2.13 与 AGENTS.md 第 5 节。
> 目标：以 `user` 域已完成的重构为模板（Entity 局部充血 + `<domain>/strategy` 策略类 + ServiceImpl 消灭私有方法），覆盖 system 模块全部 10 个域；同步收敛跨域 Mapper 直接注入与重复逻辑。
> 参照实现：`me.north30.erp.system.user`（SysUser 充血方法、user/strategy 下 5 个策略类）。

## 一、统一规则（所有域遵循）

### 1. 充血边界三铁律（Entity 方法）
1. 只校验自身状态 + 修改自身字段 + 抛 `BusinessException`；
2. 禁止调 Mapper/Service/Redis/HTTP，外部能力通过参数传入；
3. 跨实体/跨表编排留在 ServiceImpl。

### 2. strategy 粒度与命名（对齐 user 域）
- 查询构造：`XxxQueryStrategy`
- VO 装配：`XxxAssembleStrategy`
- 关联表/IO/纯函数：按职责独立命名（如 `RoleGrantStrategy`、`AuditLogDiffStrategy`、`FileStorageStrategy`）
- 位置：`<domain>/strategy` 包，不引入 service/support 中间层。

#### AssembleStrategy 与 MapStruct Converter 的边界（不重复）

一条 VO 装配链路的分工：

```
ServiceImpl 编排
  → AssembleStrategy 查数 + 组装入参（跨表取数，Strategy 的活）
    → Converter 字段映射（纯搬运，MapStruct 的活）
```

| 场景 | 归属 |
|---|---|
| Entity→VO 字段搬运、格式化、脱敏 | MapStruct Converter（不碰数据源） |
| 需要额外查库补数据（关联名/统计数/权限点）：收集外键 ID → 批量 IN 查询（避免 N+1）→ 内存 toMap/groupingBy 关联 | AssembleStrategy，结果喂给 Converter |
| 单表查询结果直接映射 VO（无变形） | 查询直接返回，可能连 Converter 都不需要 |

即：AssembleStrategy 负责"备齐 Converter 的输入"，最终出 VO 仍走各自 Converter；方案中提到 AssembleStrategy 的域均指"补数据"那一段。

### 3. 跨域 Mapper 收敛规则
- 本域关联表 Mapper（如 role 域的 roleMenu）豁免，视为本域资产；
- 跨业务域禁止直接注入 Mapper，一律走对方域 Service 接口（必要时为对方 Service 新增方法）。

### 4. 公共收敛
- **分页归一**：新增 `system/common/util/PageNormalizer`（normalizePageNum/normalizePageSize），替代 log/dict/config 等 5 处重复私有方法；保留"直调 Service 超限抛 422"的防御二层行为，相关单测勿删。
- **会话失效清理收敛**：`AuthServiceImpl.invalidateUserSessions`/`deleteByPattern` 删除，统一复用 user 域 `UserSessionRevokeStrategy`（auth → user 方向调用，符合依赖方向）。
- 时间格式化常量一律走 `DateTimeFormatUtil`，域内不再私设 `DATETIME_FORMATTER`。

### 5. 实施与验证
- 每完成一个域：跑该域单测；全量重构后统一跑全部单测（337+）验证。
- 接口签名变更（跨域收敛新增 Service 方法）同步修改测试。
- 实施顺序见下表，先易后难：先做无跨域依赖的纯函数/IO 抽取热身，再做跨域收敛最重的 role。

## 二、域清单与顺序

| 顺序 | 域 | 优先级 | 关键动作 |
|---|---|---|---|
| 1 | codesequence | 低 | 基本不动，仅 normalize 复用 |
| 2 | importtask | 低 | 不动 |
| 3 | dict | 低 | normalize 收敛、双向 Mapper 依赖处理 |
| 4 | config | 中 | SysConfig 充血 validateValueByType |
| 5 | log | 中 | 抽 AuditLogDiffStrategy（纯函数） |
| 6 | attachment | 中 | 抽 FileStorageStrategy（IO 类） |
| 7 | dept | 高 | SysDept 充血 + 树策略 + 收敛 user Mapper |
| 8 | menu | 高 | SysMenu 充血（类型配套校验）+ 树策略 |
| 9 | role | 高 | SysRole 充血 + 收敛 4 个跨域 Mapper |
| 10 | auth | 高 | 会话清理收敛 + LoginLogStrategy 抽取 |

---

## 三、各域方案（按实施顺序）

### 1. codesequence 域

现状：`SysCodeSequenceServiceImpl` 98 行，无跨域依赖，无复杂私有方法。

- **基本不动**。无充血空间（单号生成是编排逻辑不是实体状态），无 strategy 抽取价值（避免为拆而拆）。
- 仅在编码风格明显偏离（如手写格式化、魔法值）时顺手清理。
- 作为最早实施的一个域，用于验证"允许域判定为不动"的出口，避免全量机械套模板。

### 2. importtask 域

现状：`SysImportTaskServiceImpl` 43 行，无私有方法，无跨域依赖。

- **不动**。体量与复杂度均低于重构阈值。
- 若后续导入任务业务扩张（异步解析、批量落库），再评估抽 `ImportParseStrategy`。

### 3. dict 域

现状：`SysDictTypeServiceImpl` 160 行、`SysDictItemServiceImpl` 141 行。痛点最小，但存在 type↔item 双向 Mapper 依赖。

**双向依赖处理**
- `SysDictTypeServiceImpl.countItemsByTypes`（L125，注入 SysDictItemMapper 统计各 type 下 item 数）→ 新增 `SysDictItemService.countByTypes(List<String>)`，Type 侧改调该 Service；
- `SysDictItemServiceImpl` 注入 SysDictTypeMapper（校验 type 存在/启用）→ 新增 `SysDictTypeService.requireEnabledByDictType(String)`（或复用已有 requireXxx），Item 侧改调该 Service；
- 消除两 Service 互插对方 Mapper。

**normalize 收敛**：两侧的 normalizePageNum/normalizePageSize 删除，改用 `PageNormalizer`。

**不做的事**：不为 dict 抽 strategy（无查询构造/VO 装配复杂度）；不新增 Entity 充血方法（无自身状态校验规则）。

测试：计数/启用校验用例随方法归属迁移，行为断言不变。

### 4. config 域

现状：`SysConfigServiceImpl` 186 行，2 类私有方法：`validateValueByType`（L138）、normalize 分页。

**Entity 充血（SysConfig）**
- `validateValueByType()` 上移：VALUE_TYPE_NAMES 映射与按 valueType 校验 value 格式（数字/布尔/JSON/字符串）的规则随迁，非法抛 BusinessException；纯自身字段校验，符合充血边界。
- Impl 侧删除对应私有方法与常量 Map。

**normalize 收敛**：删除 normalizePageNum/normalizePageSize，改用 `PageNormalizer`（422 防御行为不变）。

**SYSTEM_BUILTIN 保护**：系统内置配置禁改/禁删的校验是"查库事实 + 自身状态"混合：事实部分（查库确认 builtin）留 Impl，状态部分（builtin 拒绝修改）可在 SysConfig 增加 `requireMutable()` 充血方法（可选，若现状已足够清晰则不强加）。

测试：valueType 与 value 格式不匹配的 422 用例改为直测 SysConfig.validateValueByType；其余接口用例不变。

### 5. log 域

现状：`SysLogQueryServiceImpl` 202 行（7 私有方法）、`AuditLogStoreImpl` 72 行（不动）、`SysLoginLogServiceImpl` 24 行（不动）。

**strategy 抽取（log/strategy）**

| 策略类 | 职责 | 吸收逻辑 |
|---|---|---|
| `AuditLogDiffStrategy` | 变更字段 diff 解析（纯函数，无 Spring 依赖）：`resolveDiffFields`、`collectDiff`、`parseJsonObject`，JSON_MAPPER 常量随迁 | L140/L159/L176 |
| `LogQueryStrategy`（可选） | 审计/登录日志查询条件构造（含 `parseTime` 的时间解析与 endExclusive 语义） | L121 |

**normalize 收敛**：删除 normalizePageNum/normalizePageSize，改用 `PageNormalizer`（防御二层 422 行为不变）。

**其他**
- DATE_TIME_FORMATTER/DATE_FORMATTER 常量改走 `DateTimeFormatUtil`（endExclusive 场景需确认工具类支持"当日 23:59:59"或用 plusDays 首含尾排）。
- 若 LogQueryStrategy 抽取后仅剩 1-2 个小方法，允许不拆，保持简单。

测试：diff 字段解析（增/删/改/嵌套）用例迁至 AuditLogDiffStrategy 直测；直调 Service 超限 422 断言不变。

### 6. attachment 域

现状：`SysAttachmentServiceImpl` 227 行，7 个私有方法，核心是文件 IO 与文件名/路径治理。

**strategy 抽取（attachment/strategy）**

| 策略类 | 职责 | 吸收逻辑 |
|---|---|---|
| `FileStorageStrategy` | 文件存储 IO 策略：MAX_FILE_SIZE/SUPPORTED_TYPES/MAX_FILE_NAME_LENGTH/MONTH_DIR_FORMATTER 常量随迁；`resolveFileType`、`extensionOf`、`sanitizeFileName`、`buildRelativePath`、`writeFile`、`resolveStoredPath`（L163-227 全部） | 纯 IO 与文件名治理，与业务编排解耦 |
| （业务编排留 Impl） | `requireAttachment` 迁 `SysAttachmentService.requireAttachment`（对齐 requireUser 模式）；每业务附件数上限 MAX_COUNT_PER_BIZ 校验属业务事实校验，留 ServiceImpl | |

**其他**
- storeRoot（Path）注入 FileStorageStrategy 构造器，配置来源不变；
- Impl 不再有 IO 细节，便于后续切换对象存储（OSS/MinIO）只需替换策略实现。

测试：文件名清洗（路径穿越 `../`、超长、非法扩展）用例改直测 FileStorageStrategy；上传/删除接口用例不变。

### 7. dept 域

现状：`SysDeptManageServiceImpl` 242 行，私有方法 resolveParent/buildAncestors/inAncestorChain/cascadeRefreshDescendants，注入 `SysDeptMapper` + `SysUserMapper`（跨域）。

**Entity 充血（SysDept）**
- `buildAncestors(String parentAncestors)`：祖先路径拼接（自身字段计算），从 L203 迁入；
- `changeParent`/`changeStatus` 写入口自带状态校验（如禁将自身设为父级这类纯字段判断的部分）。

环检测（`inAncestorChain`）与级联刷新依赖全量部门 Map，属跨实体编排，不充血。

**strategy 抽取（dept/strategy）**

| 策略类 | 职责 |
|---|---|
| `DeptTreeStrategy` | 父级解析、环检测（inAncestorChain）、级联刷新子孙（cascadeRefreshDescendants）、MAX_DEPT_LEVEL=5 层级上限校验 |
| `DeptQueryStrategy`（可选） | 列表查询条件构造；树装配与 `DeptTreeUtil` 合并去重 |

**跨域 Mapper 收敛**：`SysUserMapper`（删除部门前校验挂靠用户）→ 新增 `SysUserService.countByDeptId(Long)`，dept 域改调该 Service。

**ServiceImpl 目标形态**：公开编排 + `resolveParent`（查库业务事实校验，可迁 `SysDeptService.requireDept` 供父子双查复用）。

测试：buildAncestors 拼接、环检测、层级上限用例保持，改指向新归属；删除有用户的部门 422 用例不变。

### 8. menu 域

现状：`MenuManageServiceImpl` 267 行，7 个私有方法，注入 `SysMenuMapper` + `SysRoleMenuMapper`。

**Entity 充血（SysMenu，本轮最典型）**

SysMenu 的"菜单类型与 path/component/perms 配套校验"是纯自身状态校验，完美符合充血边界：
- `validateTypeFields()`：TYPE_DIR 无 path、TYPE_MENU 必有 path+component、TYPE_BUTTON 必有 perms 等配套规则（原 `validateTypeFields` L204）；
- `requireTypeValid()`（或 `changeType` 内校验）：menuType ∈ {1,2,3}（原 `validateMenuType` L194）；
- `changePath/changeComponent/changePerms` 等写入口内部调用上述校验。

ServiceImpl 删除 `validateMenuType`/`validateTypeFields`/`hasText`。

**strategy 抽取（menu/strategy）**

| 策略类 | 职责 |
|---|---|
| `MenuTreeStrategy` | 菜单树装配：`menuSortComparator`、`attachChildren`（自底向上递归）迁入；与 `common/MenuTreeUtil` 合并去重（若 MenuTreeUtil 已覆盖则只保留一处） |
| `MenuQueryStrategy`（可选） | 列表/树查询条件构造 |

**留在 ServiceImpl 的逻辑（跨实体编排）**
- `requireMenu`（迁 `SysMenuService.requireMenu`，对齐 requireUser 模式）；
- `checkPermsUnique`（查库唯一性，业务事实校验）；
- `validateParent` 防环（跨实体链路遍历，依赖全量菜单 Map）。

**跨域 Mapper**：`SysRoleMenuMapper` 属 role 域资产：menu 删除前的"已被角色引用则禁删"校验 → 新增 `SysRoleMenuService.existsByMenuId(Long)` / `countByMenuId(Long)`，menu 域走该 Service。

测试：类型配套校验用例改为直接测 SysMenu 充血方法；删除被角色引用菜单的 422 用例保持不变（走 Service 层）。

### 9. role 域

现状：`RoleManageServiceImpl` 295 行，8 个私有方法，注入 5 个 Mapper（role/userRole/roleMenu/menu/dataScope），跨域耦合最重。

**Entity 充血（SysRole）**
- `changeDataScope(Integer dataScope)`：携带 `VALID_DATA_SCOPES = Set.of(1,2,3,4,5,6,9)` 常量，校验非法值抛 BusinessException。
- 删除 ServiceImpl 中 `validateDataScope`，DTO 入口校验与 Entity 充血二选一保留 Entity 侧（防御二层）。

**strategy 抽取（role/strategy）**

| 策略类 | 职责 | 吸收的私有方法/逻辑 |
|---|---|---|
| `RoleQueryStrategy` | 列表分页查询构造（orderBy 白名单 ORDER_BY_WHITELIST、条件拼装） | 查询构造段 |
| `RoleAssembleStrategy` | 详情 VO 装配（joinIds/parseIds/hasText 等字符串工具内聚于此） | `joinIds`、`parseIds`、`hasText`、`isEmpty` |
| `RoleGrantStrategy` | 角色-菜单授权全删全插、角色-数据范围全删全插（本域关联表，持有 roleMenu/dataScope Mapper） | 删除角色时的关联清理（L176-180）、授权（L192-200）、数据范围（L223-230） |

**跨域 Mapper 收敛**
- `sysUserRoleMapper`（L108 关联角色列表、L170 删前统计挂靠用户数）：
  - 统计挂靠用户 → 新增 `SysUserRoleService.countUsersByRoleId(Long)`；
  - 关联角色查询 → 已有 `SysUserRoleService`，补齐所需方法。
- `sysMenuMapper`（L192 批量查菜单）→ `SysMenuService.listByIds`（已有类似能力则复用）。
- `sysRoleMenuMapper`、`sysRoleDataScopeMapper`：role 域本域关联表，豁免，收进 RoleGrantStrategy。

**ServiceImpl 目标形态**：保留公开方法编排 + requireRole（迁至 `SysRoleService.requireRole`，与 user 域 requireUser 模式对齐），私有方法全部消灭。

测试：`changeDataScope` 非法值用例迁至 Entity/Service 层断言；跨域新增 Service 方法的桩/集成测试同步补充。

### 10. auth 域

现状：`AuthServiceImpl` 366 行（5 私有方法）、`UserAccessServiceImpl` 219 行（4 私有方法）。

**会话清理收敛（本轮重点）**
- 删除 `AuthServiceImpl.invalidateUserSessions`（L350）与 `deleteByPattern`（L359）；
- auth 改调 user 域 `UserSessionRevokeStrategy`（user/strategy 已有，含 Redis 降级处理），auth → user 方向调用符合依赖方向；
- user 域停用/改密/重置密码与 auth 登出/改密共用同一清理实现。

**AuthServiceImpl strategy 抽取（auth/strategy）**

| 策略类 | 职责 | 吸收逻辑 |
|---|---|---|
| `LoginLogStrategy` | 登录日志异步记录 | `recordLoginLog`（L328） |
| `PasswordExpiryStrategy`（可选，纯函数） | 密码过期判定 | `isPasswordExpired`（L320）；若规则简单可并入 Auth 编排，不强拆 |
| `parseRefreshToken` | JWT 解析属 Token 能力，若 `JwtTokenProvider`（common）已有对应方法则收敛过去，否则留 Impl | L308 |

`DATETIME_FORMATTER` 常量改走 `DateTimeFormatUtil`。

**UserAccessServiceImpl（读模型装配器，保留模式）**
- 此前已定：保留其作为权限读模型装配器的定位。私有方法处理：
- `loadEnabledRoles`/`loadPerms`/`resolveWidestDataScope`/`expandWithAncestors` → 抽 `auth/strategy/AccessAssembleStrategy`（含 DATA_SCOPE_ORDER 常量），Impl 只留编排；
- 若抽后 Impl 与 Strategy 合计行数无净减少，允许保留 2-3 个小私有方法，不强求归零。

测试：登录/登出/改密后 Redis 会话被清的用例不变（断言行为而非实现）；权限点、数据宽度解析用例改指向 AccessAssembleStrategy。

---

## 四、验收标准

1. 除 role 域的 roleMenu/dataScope（本域关联表）外，system 模块无跨域 Mapper 注入；
2. ServiceImpl 私有方法只剩"编排"本身，无校验/装配/查询构造私有方法；
3. 全部单测通过；无新增编译警告；
4. 重构结论沉淀进《ERP系统开发编码规范文档》与 AGENTS.md（充血三铁律、strategy 粒度、跨域收敛规则）。
