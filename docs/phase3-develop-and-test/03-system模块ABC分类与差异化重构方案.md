# system 模块 ABC 分类与差异化重构方案

> **状态：定稿（2026-10-11）**。
> 背景：system 模块已完成小 DDD 重构（Entity 局部充血 + strategy + 跨域 Mapper 收敛，见《system模块小DDD重构方案》），但存在两极问题：部分域过度设计（A 类域套了编排层/策略模板），部分域欠收敛（user 域跨域 Mapper 硬违规、Manage 层残留内联校验，见《02-system模块分层校验收敛方案》）。
> 命名决策（2026-10-11 定稿）：编排层统一命名 **ApplicationService**，领域基础服务统一命名 **DomainService**，保留 Sys 前缀。

## 一、模块分类表

| 域 | 分类 | 满足的判定条件 | 建议目录结构 |
|---|---|---|---|
| user | **B** | 唯一性/跨表校验（dept 存在性、角色引用计数）①；关联 2 张表（dept、userRole）②；简单状态变化（changeStatus）③；业务规则 5+（管理员保护、密码策略、会话失效）④；被 auth/dept 调用且有写操作⑤ | `user/{controller, service(App+Domain), mapper, entity, dto, vo, converter, strategy}` |
| role | **B+（偏 C，暂不升级）** | 聚合跨 3+ 表（role/userRole/roleMenu/dataScope）①；跨聚合协调（授权联动 menu、删除校验 user）②；授权全删全插有事务一致性要求③；业务规则 8+④ | 同上 + `strategy`（RoleQuery/RoleAssemble/RoleGrant 已具备，等价 C 类的领域层，**不引 Repository/事件**） |
| menu | **B** | 跨表校验（父级合法、防环、perms 唯一）①；关联 2 表（roleMenu）②；类型配套状态规则③；规则 6+（按钮禁建子、层级、类型字段配套）④；被 role/auth 调用⑤ | 同上 |
| dept | **B** | 跨表校验（环检测、级联重算）①；关联 2 表（user 挂靠计数）②；树状态变化③；规则 5+（层级上限、编码唯一、级联刷新）④；被 user/auth 调用⑤ | 同上（DeptTreeStrategy 已具备） |
| auth | **B** | 跨表/跨域协调（user/role/menu/dept 五域取数）①；状态变化（token 签发/会话失效）②；规则 6+（密码过期、账号锁定、验证码、会话清理）③ | `auth/{controller, service(App+Domain), dto, vo, strategy}`（无 Entity/Mapper，纯编排读模型） |
| log | **A** | 90% 是追加写+查询①；Entity 单表②；校验仅格式③；无状态流转④ | `log/{controller, service, mapper, entity, dto, vo, converter, strategy}`（保留 AuditLogDiffStrategy 纯函数） |
| dict | **A** | 90% CRUD①；单表（type/item 各自单表）②；校验仅格式/存在性③；无状态流转④ | `dict/{controller, service, mapper, entity, dto, vo, converter}`（**撤销 ManagementService**） |
| config | **A** | 90% CRUD①；单表②；校验仅格式（valueType）③；无状态流转④ | 同上（**撤销 ManagementService**，SysConfig.validateValueByType 充血保留） |
| attachment | **A** | 校验仅格式/上限①；无状态流转②；无跨域③ | `attachment/{controller, service, mapper, entity, dto, vo, converter, strategy}`（FileStorageStrategy 保留，IO 收纳有价值） |
| importtask | **A** | 90% CRUD①；单表②；无校验/无状态③ | `importtask/{controller, service, mapper, vo}` |
| codesequence | **A** | 90% CRUD①；单表②；无校验③ | `codesequence/{controller, service, mapper, entity, dto, vo, converter}` |

> 说明：role 达到 C 类门槛（3 条），但其"复杂度"集中在**关联表全删全插**这一种模式，已由 RoleGrantStrategy 妥善收纳；引入 Repository/领域事件属过度设计，维持 B 类目录 + 策略类的轻量实现，标注 B+。

## 二、各域问题诊断与重构方向

### B 类域（ApplicationService + DomainService 双层）

**通病（四域共有）**
1. 编排层命名不统一：`UserManagementService` / `RoleManageService` / `MenuManageService` / `SysDeptManageService` 四种拼法并存 → 统一改名 `SysXxxApplicationService`；
2. 领域基础服务与编排层职责边界靠约定无命名区分 → `SysXxxService` 改名 `SysXxxDomainService`；
3. Manage 层残留内联事实校验与私有校验方法（02 方案 §3 已逐条列明）。

**user**
- 问题：跨域硬违规——直插 `SysDeptMapper` 查组织存在性（18024）；update/delete 内联管理员保护；`UserRoleStrategy.countByUserId` 绕过 role 域 Service；changeStatus 双层兜底待二选一。
- 方向：执行 02 方案 Commit ①②；改名 `SysUserApplicationService` + `SysUserDomainService`。

**role**
- 问题：`requireScopeValid` 私有校验未下沉（scopeType 值域→Entity、档位 9 跨字段→RoleGrantStrategy.buildDataScope）；角色编码唯一内联 selectCount；5 个 Mapper 注入需复核是否全部必要。
- 方向：02 方案 §3.2 处置；改名 `SysRoleApplicationService` + `SysRoleDomainService`；RoleGrant/Query/Assemble 三策略保留。

**menu**
- 问题：`checkPermsUnique`/`requireParent`/`validateParent` 三个私有校验未收敛进 `SysMenuService`；"按钮下禁建子"规则重复两处。
- 方向：02 方案 §3.3（`requirePermsAvailable`/`requireValidParent`）；改名 `SysMenuApplicationService` + `SysMenuDomainService`。

**dept**
- 问题：create/update/delete 内联编码唯一与存在性校验，绕过本域 Service 守卫。
- 方向：02 方案 §3.4（`requireDeptCodeAvailable`/`requireDept`）；DeptTreeStrategy 环检测不动；改名 `SysDeptApplicationService` + `SysDeptDomainService`。

**auth**
- 问题：整体健康（会话清理已收敛至 UserSessionRevokeStrategy、LoginLogStrategy 已抽取）；UserAccessServiceImpl 作为读模型装配器定位清晰。
- 方向：仅随命名统一改为 `SysAuthApplicationService`（auth 无对外跨域消费方，不设 DomainService，避免空壳）。

### A 类域（单层 Service，去掉多余分层）

**Entity 充血同样适用于 A 类（2026-10-11 补充定稿）**
- 分类只决定**分层**（A 类不拆 ApplicationService/DomainService），不豁免**充血**：只要 Service 里出现"自身字段校验/缺省兜底"的 if 块，就应下沉 Entity 充血方法，与 B 类同一标准。
- 两类候选（均为纯自身字段操作，符合充血三铁律）：
  1. **格式/值域校验**：如 `SysConfig.validateValueByType()`（已定）；
  2. **缺省兜底**：`fillDefaults()` 类方法——dict item 的 itemSort/isDefault/status 三连 if、dict type 的 status 兜底、user 的 gender/status 兜底均属此类，下沉后 Service 编排只剩一行调用。
- 判定口径：if 块只读写**本实体字段**且无查库 → 下沉 Entity；需要查库/跨实体 → 留 Service（requireXxx 守卫或 Strategy）。

**dict / config（过度设计典型）**
- 问题：`DictManagementService` + `SysDictTypeService`/`SysDictItemService` 三层并存，但 dict/config 是纯 CRUD——ManagementService 只是零价值转发，双 Service 属模板套用；双向 Mapper 依赖已收敛，无剩余痛点；此外 item/type 的缺省兜底 if 散落 Service（见上，下沉 `fillDefaults()`）。
- 方向：**撤销 ManagementService**，Controller 直调单层 `SysDictTypeService`/`SysDictItemService`/`SysConfigService`；A 类域不改名 DomainService（单层即 DomainService 本身，无需后缀区分）。

**log / attachment / importtask / codesequence**
- log：AuditLogDiffStrategy 纯函数保留（有真实复用价值）；其余不动。
- attachment：FileStorageStrategy 保留（IO 治理 + 未来换 OSS 的替换点）；`SysAttachmentServiceImpl` 内联判空按既定结论删除（Controller @Valid 已覆盖）。
- importtask / codesequence：完全不动（43/98 行，低于一切重构阈值）。

## 三、跨模块依赖分析

| 调用方 → 被调方 | 读/写 | 处理方式 |
|---|---|---|
| auth → user（登录查用户、会话失效） | 读 + 写（会话清理） | 走 `SysUserDomainService` 最小契约 |
| auth → dept / role / menu（权限装配） | 读 | 走各域 DomainService 只读接口 |
| auth → log（登录日志异步写） | **写** | 走 `SysLoginLogService` 单层接口（log 为 A 类，写接口即其 Service 本身） |
| dept → user（删部门前挂靠计数） | 读 | `SysUserDomainService.countByDeptId`（已收敛） |
| menu → role（删菜单前引用检查） | 读 | `SysRoleMenuService.countByMenuId`（已收敛） |
| role → menu（授权批量查菜单） | 读 | `SysMenuDomainService.listByIds`（已收敛） |
| user → dept（组织存在性守卫） | 读 | **未收敛**：改调 `SysDeptDomainService.requireDept`，撤 Mapper 直插（02 方案 #1） |
| user → role（删用户前引用计数） | 读 | `SysUserRoleService.countUsersByRoleId` 收编策略类直插 |
| 未来 business 各域 → codesequence（取单号） | **写**（nextSequence 递增） | 单层 `SysCodeSequenceService` 即准入接口，无需额外分层 |
| 未来 business 各域 → dict / attachment | 读 | 同上，单层 Service 直调 |

结论：A 类域被跨域调用的场景，其**单层 Service 就是准入清单**——不因"被调用"而升级为 B 类双层，只要求接口最小化（requireXxx/countByXxx/listByXxx）。

## 四、重构优先级

| 级别 | 域 | 动作 |
|---|---|---|
| **P0**（进行中/有硬违规） | user、role、menu、dept | 02 方案 Commit ①②：跨域 Mapper 收敛 + 守卫进 DomainService + Entity 充血 + Strategy 下沉 |
| **P1**（被多域依赖，改名影响面大） | 全体 B 类 + auth | 命名统一：`SysXxxManagementService`→`SysXxxApplicationService`（4 个）、`SysXxxService`→`SysXxxDomainService`（B 类 5 个）；单独 refactor commit，Controller/测试成套改 |
| **P2**（过度设计回退，中等收益） | dict、config | 撤销 ManagementService 双层，Controller 直调单层 Service |
| **P3**（简单且稳定，不动） | log、attachment、importtask、codesequence | 仅 attachment 按既定结论删 2 处死代码/内联判空，其余零改动 |

## 五、实施顺序建议

1. P0（02 方案两步 commit，行为不变）；
2. P1（纯改名 commit，编译期可验证）；
3. P2（dict/config 回退 + A 类 Entity 缺省兜底/格式校验下沉 `fillDefaults()` 等，删代码与搬方法为主）；
4. 每步 `mvn clean compile` + 全量 338 单测。
