# system 模块分层校验收敛方案

> **状态：方案定稿（2026-10-09），待执行。**
> 背景：四个 Manage 实现（user/role/menu/dept）中仍存在私有校验方法与内联事实校验；user 域存在跨域注入 `SysDeptMapper` 的硬规则违规；部分守卫绕过本域 Service 内联重写。
> 目标：ManagementService 收敛为**纯业务编排层**（零私有校验方法、零内联事实校验），每类校验有唯一归属。

## 一、分层校验归属原则（四分法）

| # | 校验类型 | 归属 | 错误码 | 示例 |
|---|---|---|---|---|
| 1 | 格式/值域（分页范围、枚举取值） | DTO 注解 + Controller `@Valid` | 400 | `@Min(1)/@Max(200)`、status 0/1 |
| 2 | 实体自身状态 / 实体跨字段规则 | Entity 充血方法 | 422 | 管理员保护、菜单 type 必填字段、scopeType 值域 |
| 3 | 事实型查询（存在性、唯一性、引用计数） | 本域 Service `requireXxx` | 422 | `requireUser`、`requireRoleCodeAvailable` |
| 4 | 批次/集合级检查（N 个 ID 中的非法项、树遍历） | Manage 编排 / 本域 Strategy | 422 | `requireAllExist`、dept 环检测 |

**判定口诀**：
- 校验问题**脱离本次用例是否仍有意义**？是 → Service/Entity；否（带用例上下文的变体，如"更新时排除自己"的唯一性）→ Service 提供 exclude 参数 或 留在 Manage/Entity。
- 需要跨节点/查库的纯规则（环检测、级联重算）→ Strategy；只看自身字段状态 → Entity。
- **跨字段业务规则不进 DTO 注解**（业务逻辑入侵 DTO，且 422→400 错误码漂移）——留在 Entity 或 Strategy。

## 二、Manage Mapper 注入边界（与校验归属配套）

| 动作 | 允许 |
|---|---|
| 跨业务域 Mapper 注入 | ❌ 硬禁止，一律走对方域 Service |
| 本域聚合用例写库（insert / updateById / deleteById / 分页执行） | ✅ 允许（反向禁止的代价是 Service 膨胀成 CRUD 镜像 + 零价值转发） |
| 守卫事实查询 | 必须经本域 Service `requireXxx`，禁止内联 `selectCount/selectById` 重写 |
| Strategy 跨域取数 | ❌ 必须走对方域 Service（"同模块直插 Mapper"豁免修正为**仅限本域关联表资产**） |
| check-then-act 原子性 | 不靠调用路径，靠乐观锁 `version`——"走 Service 防绕过守卫"不成立，守卫收敛的意义是**规则单点实现** |

## 三、逐条清单与处置

### 3.1 UserManagementServiceImpl（私有方法 0，内联校验 6 处）

| # | 位置 | 校验内容 | 错误码 | 处置 |
|---|---|---|---|---|
| 1 | create L125-127 / update L163-165 | 组织存在性（直插 `SysDeptMapper`） | 18024 | **⚠️ 跨域 Mapper 硬违规**：改调 `SysDeptService.requireDept`，删除 `sysDeptMapper` 注入 |
| 2 | update L160-162 | 停用内置管理员（资料更新路径未走 changeStatus） | 18012 | 下沉 Entity：`SysUser` 新增守卫方法 |
| 3 | delete L189-191 | 内置管理员不可删除 | 18012 | 下沉 Entity：与 #2 合并为同一守卫（或变体） |
| 4 | delete L193-196 | 被角色引用不可删（经 `UserRoleStrategy.countByUserId`） | 18013 | 收编：改调 `SysUserRoleService.countUsersByRoleId`，策略类撤 Mapper 直插 |
| 5 | changeStatus L215-217 | status 取值 0/1 兜底（注解已校验） | 10001 | 下沉 Entity 并入 `changeStatus`，或按"防御二层"留原地——二选一，禁止两处并存 |

### 3.2 RoleManageServiceImpl（私有方法 1，内联校验 1 处）

| # | 位置 | 校验内容 | 错误码 | 处置 |
|---|---|---|---|---|
| 1a | requireScopeValid L202-205 | scopeType ∈ `SysRole.VALID_DATA_SCOPES` | ROLE_DATA_SCOPE_INVALID | 下沉 Entity：`SysRole.requireValidScopeType` 静态方法 |
| 1b | requireScopeValid L206-210 | 档位 9 必须勾选组织或人员（跨字段） | ROLE_DATA_SCOPE_INVALID | 下沉 `RoleGrantStrategy.buildDataScope`（构建时校验，"先循环校验再循环构建"两趟合一趟）；**不进 DTO** |
| 2 | create L89-94 | 角色编码唯一（内联 selectCount） | ROLE_CODE_EXISTS | 本域 Service：`SysRoleService.requireRoleCodeAvailable` |

### 3.3 MenuManageServiceImpl（私有方法 3）

| # | 位置 | 校验内容 | 错误码 | 处置 |
|---|---|---|---|---|
| 1 | checkPermsUnique L137-148 | perms 唯一（excludeId 排除自身） | MENU_PERMS_DUPLICATED | 本域 Service：`SysMenuService.requirePermsAvailable(perms, excludeId)` |
| 2 | requireParent L153-159 | 父级存在 | MENU_NOT_FOUND | 本域 Service：复用 `SysMenuService.requireMenu` |
| 3 | validateParent L164-188 | 父级非按钮 + 防环回溯（循环查库） | MENU_LEVEL_EXCEEDED / 10001 | 合并进 `SysMenuService.requireValidParent(selfId, parentId)`（selfId=null 表新增）；吸收 create L65-69 的"按钮下禁建子节点"重复规则 |

### 3.4 SysDeptManageServiceImpl（私有方法 0，内联校验 3 处）

| # | 位置 | 校验内容 | 错误码 | 处置 |
|---|---|---|---|---|
| 1 | create L70-74 | deptCode 唯一（内联 selectCount） | DEPT_CODE_EXISTS | 本域 Service：`SysDeptService.requireDeptCodeAvailable` |
| 2 | update L91-94 / delete L129-132 | 组织存在性（内联 selectById） | DEPT_NOT_FOUND | 本域 Service：`SysDeptService.requireDept`（守卫绕过 Service，收敛） |
| 3 | update L97-111 | 自身/环检测 + 祖级级联重算 | 10001 | ✅ 不动——已在 `DeptTreeStrategy`，跨节点逻辑的正确归属 |

### 3.5 汇总

| 归属 | 项数 | 内容 |
|---|---|---|
| 本域 Service 新增/复用 | 6 | role 编码唯一、dept 编码唯一、dept 存在性、perms 唯一、菜单父级存在、菜单父级合法（含防环） |
| Entity 充血 | 3 | 管理员保护（更新/删除）、status 取值兜底（可并入 changeStatus）、scopeType 值域 |
| Strategy 下沉 | 1 | 档位 9 跨字段规则 → `RoleGrantStrategy.buildDataScope` |
| 跨域 Service 收敛 | 1 | user 组织存在性 → `SysDeptService.requireDept`（撤 SysDeptMapper 注入） |
| 既有收编 | 1 | `UserRoleStrategy.countByUserId` → `SysUserRoleService` |
| 不动 | 1 | dept 环检测/级联（已在 DeptTreeStrategy） |

## 四、错误码与 API 文档影响

- 全部处置走 `BusinessException` → **422 路径不变**；事实校验只是从 Manage 移到 Service/Entity，语义与错误码值均不变。
- **无 400/422 漂移，API 文档无需修改**；接口返回结构不变，前端无感知。
- 本方案不含"校验上移 DTO 注解"的动作（那会触发 422→400，需单独评估与文档同步）。

## 五、实施与验证

- **Commit ①（refactor）**：硬规则违规 + Service 守卫收敛——user 组织存在性走 `SysDeptService`、role/dept 编码唯一与存在性、menu perms/父级守卫进 `SysMenuService`/`SysRoleService`/`SysDeptService`。
- **Commit ②（refactor）**：Entity 充血（管理员保护、status 兜底、scopeType 值域）+ Strategy 下沉（档位 9）+ `UserRoleStrategy` 收编至 `SysUserRoleService`。
- 每步执行 `mvn clean compile` + 全量单测（当前 338 个）验证；涉及签名变更的测试同步修改。
- 完成后在本文件头部更新状态为"已执行"。

## 六、关联未决事项

- **ManagementService 命名统一**（`SysUserManagementService` 等 4 个，补 Sys 前缀 + Management 拼法统一）：方案已讨论达成倾向，待确认后单独 refactor commit，不与本方案混提。
- 守卫层双接口是否引入 `DomainService` 后缀（方案二）：仅讨论，未决。
