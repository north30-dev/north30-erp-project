# 09 - system 模块自我操作与提权风险分析报告

> 日期：2026-10-30
> 范围：erp-system 模块（D2 阶段已交付的 auth/user/role/menu/dept/dict/config/attachment/log/importtask 十个业务域）
> 起因：普通管理员（sysadmin，持 SYSTEM_ADMIN 角色，is_admin=0）在用户管理界面停用自己后原地登出且无法再登录，只能靠内置超管恢复。由此展开对"自我操作/不可自愈削弱/提权"类问题的全量排查。
> 方法：按 Controller 层全部写接口（30+ 个）逐个核对 Service 实现层守卫，结论均附代码证据。

## 1. 背景与权限模型

- 权限点 = 启用角色的并集；`is_admin=1` 或持 ADMIN 角色则代码层全量（`UserAccessServiceImpl.loadByUserId`）。
- 内置角色：ADMIN（超级管理员）、SYSTEM_ADMIN（系统管理员，预勾选全部 /system 菜单），`is_builtin=1`。
- 内置账号：`admin`（is_admin=1，绑定 ADMIN 角色）、`sysadmin`（is_admin=0，绑定 SYSTEM_ADMIN 角色），由 AdminInitializer 幂等创建。
- 会话机制：Redis 会话白名单（`system:session:{userId}:{jti}`），每次请求实时校验会话存在性 + 用户状态/权限点（`JwtLoginUserConverter`），因此**权限与状态变更实时生效**。

## 2. 已确认的漏洞与缺口

### P0 🔴 resetPassword 可重置内置超管密码（提权接管）（已修复：批次一）

- 位置：`SysUserApplicationServiceImpl.resetPassword`
- 问题：仅调 `requireUser`，无 `requireNotAdmin()`。sysadmin 可重置 `admin` 账号密码 → 获取返回的初始密码 → 以超管身份登录 → 完全绕过 18012/18040 全部防提权设计。
- 修复：加 `requireNotAdmin()`（18012）。

### P1 🟡 changeStatus 可停用自己（登录锁死，本次事故）（已修复：批次二）

- 位置：`SysUserApplicationServiceImpl.changeStatus` → `SysUser.changeStatus`
- 问题：停用分支只有 `requireNotAdmin()`，不校验目标是否为操作者本人。sysadmin 停用自己后：
  1. `UserSessionRevokeStrategy.revoke` 删除自己全部 Redis 会话 → 原地登出；
  2. 登录接口校验 `status != 1` 抛 ACCOUNT_DISABLED → 无法再登录；
  3. refresh 接口同样拦截 → 只能靠超管启用恢复。
- 修复：Service 层校验 `当前登录人 != 目标用户`，禁止停用自己。

### P1 🟡 assignRoles 可摘除自己持有的 ADMIN 类角色（自我降权无法自愈）（已修复：批次二）

- 位置：`SysUserApplicationServiceImpl.assignRoles` → `requireAssignAdminAllowed`
- 问题：18040 守卫只在**目标列表包含 ADMIN 角色**时要求超管；清空/摘除自己的 ADMIN 角色不触发。摘除后操作者变为无权限用户，能登录但什么都干不了，恢复入口（用户管理）恰好也在被摘掉的权限里 → 需要其他管理员救助。
- 修复：非超管操作者禁止从目标列表移除自己当前持有的 ADMIN 角色。

### P1 🟡 assignMenus 可清空自己持有角色的菜单授权（功能锁死）（已修复：批次二）

- 位置：`SysRoleApplicationServiceImpl.assignMenus`
- 问题：ADMIN 角色授权已锁死，但 SYSTEM_ADMIN（操作者自己持有的角色）可被编辑。sysadmin 清空自己角色的 /system 菜单后实时失去角色/菜单/用户管理入口，无法自愈，需超管重新授权。
- 修复：禁止修改操作者当前持有角色的菜单授权（或仅超管可改 SYSTEM_ADMIN 授权）。

### P2 🟢 delete 可删除自己（目前被偶然兜住）（已修复：批次一）

- 位置：`SysUserApplicationServiceImpl.delete`
- 现状：无自我操作守卫，但实际删不掉——能调 `system:user:delete` 的人必有角色（→ 18013 USER_REFERENCED）或 is_admin=1（→ 18012）。权限实时校验也堵住"先摘角色再删除"（摘完角色下一个请求即 403）。
- 风险：属于**碰巧被引用检查兜住**，非显式设计；权限模型调整后可能失效。
- 修复：显式禁止删除自己（同时改善"删自己报角色引用"的误导性提示）。

### P2 🟢 update 可修改内置超管资料（保护不一致）（已修复：批次一）

- 位置：`SysUserApplicationServiceImpl.update`
- 问题：changeStatus/delete 均有 `requireNotAdmin()`，update 没有。UserUpdateDTO 不含 username/userCode/password/roleIds/status，最多改部门/仓库/联系方式，危害有限，但保护不一致。
- 修复：加 `requireNotAdmin()`。

### P3 🟡 登录失败锁定可被恶意利用（DoS 超管）

- 位置：`SysAuthApplicationServiceImpl.login`（锁定按 username 计数）
- 问题：任何人可故意用错误密码刷 `admin` 账号 5 次 → 超管被锁 30 分钟，可无限循环续锁。
- 修复：见第 2.1 节"登录链路防爆破加固"，与本项合并处理。

### 2.1 🟡 附加：验证码形同虚设，登录链路防爆破需整体加固

视觉层（`CaptchaUtil`）：Hutool LineCaptcha，4 位标准印刷体 + 仅 6 条直线干扰，无扭曲/粘连/噪点。对 `ddddocr` 等专用 OCR 库识别率接近 99%，多模态 AI 视觉可直接识别。

但视觉弱不是关键，**架构上验证码对自动化攻击的边际成本 ≈ 0**：

1. `/auth/captcha` 匿名接口且无频控：攻击者每次尝试重新取码 + OCR，单次成本 = 2 个 HTTP 请求 + 1 次毫秒级 OCR；
2. Redis 故障降级时验证码校验直接跳过（login 第 91-94 行），与失败计数同时失效 → "降级=裸奔"的爆破窗口；
3. GETDEL 一次性（防重放）是唯一做对的点，但挡不住"每次重新取码"；
4. `/auth/login` 本身无 IP 限速，爆破吞吐只受带宽限制。

当前思路把最弱的验证码当主防线，把可被武器化的账号锁定当主防线——**主防线应该建立在 IP 维度**。

**加固方案（P3 展开为"登录链路防爆破加固"）**：

| 优先级 | 措施 | 效果 |
|--------|------|------|
| 1 | 登录接口按 IP 限速（如 10 次/分钟滑动窗口） | 封死爆破吞吐，性价比最高 |
| 2 | 取码接口按 IP 频控（如 30 次/分钟） | 防止绕过登录限速刷码 |
| 3 | 降级模式兜底：Redis 不可用时限流用本地 Caffeine 计数，不跳过 | 堵住降级裸奔窗口 |
| 4 | 渐进式验证码：失败 N 次内不要求验证码，超阈值才升级 | 正常用户体验不变，攻击者成本陡增 |
| 5 | 锁定/密码错误统一文案与错误码（18001），细节进日志 | 防用户名存在性探测 |
| 6 | 高权账号（admin）被锁记 WARN 告警 | 可观测 |

原有 P3"锁定改 IP+用户名双维度"并入上表第 1 项，另保留：用户名维度计数阈值放宽（如 20 次）防针对性爆破，避免人际恶作剧式锁号。

## 3. 已有兜底、确认无需处理的项

| 项 | 证据 |
|----|------|
| 删除/停用内置角色 | `is_builtin=1` → 18017 / ROLE_BUILTIN |
| 修改 ADMIN 角色菜单授权 | assignMenus 显式拒绝（权限代码内置全量） |
| 给自己分配 ADMIN 角色 | 18040 要求超管 |
| 内置菜单停用/删除 | requireSubtreeNotSystem（18039）+ 子节点/角色引用双守卫 |
| 组织删除 | 下级组织 + 挂靠用户双校验 |
| 内置系统参数 | is_system=1 → 18033 不可删/停 |
| 字典删除 | 引用守卫 |
| auth 自我操作（changePassword/logout/refresh/currentUser） | 语义正确 |
| role.update 改 ADMIN 角色数据范围 | ADMIN 数据范围代码层直通，改了不生效 |
| "先摘自己角色再删除自己" | 权限点每次请求实时查，摘完即 403，到不了 delete |

## 4. 修复实施清单

| 优先级 | 位置 | 守卫 | 错误码建议 |
|--------|------|------|-----------|
| P0 | resetPassword | requireNotAdmin() | 复用 18012 |
| P1 | changeStatus（用户） | 禁止停用自己 | 新增 18041（禁止自我操作） |
| P1 | assignRoles | 非超管禁止移除自己持有的 ADMIN 角色 | 新增 18041 |
| P1 | assignMenus | 禁止修改自己持有角色的授权 | 新增 18041 |
| P2 | delete（用户） | 禁止删除自己 | 新增 18041 |
| P2 | update（用户） | requireNotAdmin() | 复用 18012 |
| P3 | 登录链路防爆破加固（IP 限速 + 降级兜底 + 渐进式验证码 + 统一文案） | 另开任务，方案见 2.1 节 |

实现要点：
- 自我操作守卫统一在 Service 层比对 `SecurityUtils.requireCurrentUser().userId()` 与目标用户 ID，不依赖角色判断（任意被授予 system:user:* 权限的角色都可能触发）。
- `requireNotSelf` 落点为 `SysUserDomainService`（领域守卫，与 requireUser 等并列），不放入 SecurityUtils——后者保持纯安全上下文职责，不背业务错误码；changeStatus/delete 各加一行调用。
- 自我操作类守卫不做在 Entity 充血方法内（实体拿不到安全上下文）。
- 前端可选优化：用户列表对当前登录人行禁用"停用/删除"按钮（后端守卫为准）。
- 同步更新：接口文档错误码表、单测（每个守卫至少 1 个正例 + 1 个反例）。

### 4.1 优化批次划分（每批次一个会话完成，避免单会话过大）

**批次一：提权与自我操作守卫（P0 + P2，后端小改 + 单测）**
- resetPassword 加 `requireNotAdmin()`（18012）
- update 加 `requireNotAdmin()`（18012）
- delete 加 requireNotSelf（18041）
- 涉及文件：`SystemErrorCode`、`SysUserDomainService`、`SysUserApplicationServiceImpl` + 对应单测
- 验收：单测通过；sysadmin 重置 admin 密码返回 18012

**批次二：停用与角色自我削弱守卫（P1，逻辑稍复杂）**
- changeStatus 停用分支加 requireNotSelf（18041）
- assignRoles：非超管禁止移除自己持有的 ADMIN 角色（18041）
- assignMenus：禁止修改自己持有角色的菜单授权（18041）
- 涉及文件：`SysUserDomainService`（requireNotSelf）、`SysUserApplicationServiceImpl`、`SysRoleApplicationServiceImpl` + 单测
- 验收：单测通过；sysadmin 停用自己/摘自己 ADMIN 角色/清自己角色菜单均返回 18041
- 前端同步：用户列表当前登录人行禁用"停用/删除"按钮（可选）

**批次三：登录链路防爆破加固（P3，独立任务，涉及新组件）**
- 登录/取码接口 IP 限速（Redis 计数 + Caffeine 降级兜底）
- 渐进式验证码（失败 N 次内不要求）
- 锁定/密码错误统一文案与错误码；admin 被锁 WARN 告警
- 用户名维度计数阈值放宽至 20 次
- 涉及文件：`SysAuthApplicationServiceImpl`、新增限流策略类 + 单测/集成测试
- 验收：限流生效、Redis 宕机时限流仍工作、爆破场景集成测试通过

批次间无依赖，可按一、二、三顺序执行；批次一/二合计改动面小，若会话余量充足可合并，但批次三必须独立。

### 4.2 实施进度（2026-11-08）

**批次一已完成（P0 + P2）**

- 错误码：`SystemErrorCode` 新增 `SELF_OPERATION_FORBIDDEN(18041, "禁止对当前登录账号执行该操作")`，全仓检索确认无占号冲突。
- 领域守卫：`SysUserDomainService.requireNotSelf(id)` 已实现（比对 `SecurityUtils.requireCurrentUser().userId()`，无登录态抛 10101）。
- 接口守卫：`resetPassword`/`update` 加 `requireNotAdmin()`（18012）；`delete` 加 `requireNotSelf()`（18041）。
- 前端：用户列表对当前登录行禁用「停用/删除」，对内置管理员行禁用「编辑/重置密码/停用/删除」。
- 接口文档：2.10 错误码表新增 18041，5.1.4/5.1.5 错误码清单同步。
- 测试：单元测试补充 6 个用例（service 3 + domain 3）；API 集成测试新增 3 个用例（修改/删除/重置密码的内置管理员或自我操作拒绝），实际执行 `Tests run: 25, Failures: 0, Errors: 0`。
- 验证命令：`mvn -pl erp-system -am test`；`mvn -pl erp-bootstrap -am test -Pit -Dtest=UserIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false`；前端 `pnpm build`。

**批次二已完成（P1）**

- `changeStatus`：停用分支增加 `requireNotSelf`（18041），停用自己返回 18041；他人停用内置管理员仍为 18012。
- `assignRoles`：新增 `requireNotRemoveOwnAdminRole`，`is_admin=0` 的操作者移除自己持有的 `ADMIN` 角色返回 18041；内置超管（`is_admin=1`）不受限。注意：「超管」判定用 `is_admin` 而非角色，因为持 `ADMIN` 角色者一旦被摘除反而失去全部入口。
- `assignMenus`：在当前登录人持有的角色上修改菜单授权返回 18041（ADMIN 角色的 ROLE_BUILTIN 拦截仍优先）。
- 接口文档：5.1.6 / 5.1.8 / 5.2.5 错误码清单补充 18041。
- 测试：单测新增 changeStatus 自我停用、assignRoles 自我降权（含内置超管放行）、assignMenus 自我清权 4 个用例；集成测试新增 3 个用例，并新增 `adminRoleOperatorToken()` 辅助（创建 `is_admin=0` 且持有 `ADMIN` 角色的操作者）。
- 验证：`mvn -pl erp-system -am test` 全绿；`mvn -pl erp-bootstrap -am test -Pit -Dtest='UserIntegrationTest,RoleIntegrationTest'` → `Tests run: 48, Failures: 0, Errors: 0`。

**批次三已完成（P3，登录链路防爆破加固）**

- 配置：`erp.auth` 新增 `login-rate-limit-per-minute=10`、`captcha-rate-limit-per-minute=30`、`captcha-required-threshold=3`、`username-fail-lock-threshold=20`；测试 profile 将前两项放大到 1000，避免集成测试同 IP 高频登录互相误伤。
- IP 限流：新增 `LoginRateLimitStrategy`，Redis ZSET 60 秒滑动窗口；捕获 `RedisConnectionFailureException` 时降级为 Caffeine 1 分钟本地计数，不裸奔放行；超限返回新增错误码 `18042 操作过于频繁`。
- 取码接口：`createCaptcha` 接入 `checkCaptcha`，按 IP 30 次/分钟频控。
- 登录链路：顺序调整为「IP 限流 → 读取失败计数 → 渐进式验证码 → 账号/停用/锁定 → 口令校验」；验证码在「已携带或失败次数 ≥3」时强制校验，未达阈值且未携带则跳过（前端始终展示并携带，正常体验不变）。
- 锁定策略：用户名维度阈值由 5 次放宽至 20 次（30 分钟窗口），仍返回 `18002`；`is_admin=1` 账号触发锁定或达到阈值时记 WARN 告警。
- 前端：`RequestError` 携带业务码；登录页验证码刷新增加 loading 与 2 秒节流，限流/参数错误时不再自动刷新验证码。
- 接口文档：2.10 错误码表新增 18042、18002 触发条件改为 20 次；4.1/4.2 说明与错误码清单同步。
- 测试：单测新增限流、渐进式验证码、锁定阈值用例，并新增 `LoginRateLimitStrategyTest`；集成测试新增「未达阈值免验证码登录」与「失败 3 次后免验证码被拒」用例。
- 验证：`mvn -pl erp-system -am test`；`mvn -pl erp-bootstrap -am test -Pit -Dtest=AuthIntegrationTest`；前端 `pnpm build`（结果见交付说明）。

## 5. 验证方法与覆盖边界声明

- 结论来自逐个读 Controller 写接口（@PostMapping/@PutMapping/@DeleteMapping 全量枚举）及对应 Service 实现的守卫代码。
- "已确认无问题"仅指本文列出的攻击路径走不通，不含并发时序（如停用与登录竞态）等另一类问题；现有 Redis 降级兜底（SYS-01）可接受。
- 本报告只覆盖 system 模块；business 模块（采购/销售/库存/生产/财务）开发后需重新审查同类问题（如"停用拥有未完结单据的用户"）。
