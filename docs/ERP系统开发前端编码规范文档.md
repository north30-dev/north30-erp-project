# ERP系统开发前端编码规范文档

| 文档属性 | 内容 |
|---------|------|
| 文档名称 | ERP系统开发前端编码规范文档 |
| 版本号 | V1.0 |
| 状态 | 生效中 |
| 适用范围 | erp-frontend 全部代码 |
| 上位文档 | AGENTS.md 第 6 节技术栈红线、ERP系统开发API设计与规范文档 |

本规范的制定背景：D2 后端曾因"多代理并行、无约定先行"产生大量一致性债务（同名异构、风格分裂、重复代码）。前端开发的最高原则是**先约定、后编码**：所有页面都是基建层与模板的变体，不允许多种写法并存。

---

## 1. 目录结构（强制）

```text
erp-frontend/src/
├── api/                    # 接口定义层：一个业务域一个文件（user.ts/role.ts…）
├── components/             # 全局公共组件，一律 App 前缀（AppSearchForm/AppCrudModal…）
│   └── <Component>/
│       ├── index.vue       # 组件入口
│       └── type.ts         # 组件对外类型（可选）
├── composables/            # 组合式函数，一律 use 前缀（useTable/useAuth…）
├── constants/              # 常量（枚举值、存储键名、菜单类型等）
├── directives/             # 自定义指令（v-perm 等），index.ts 统一注册
├── router/                 # 路由表 + 守卫
├── stores/                 # Pinia：一个领域一个 store（auth/app…），组合式写法
├── types/                  # 全局类型：api.d.ts（接口契约类型）
├── utils/                  # 工具（request.ts 请求封装等），纯函数、无业务语义
└── views/                  # 页面，与后端业务域分包对齐
    └── system/
        ├── user/index.vue          # 列表页
        ├── user/components/        # 页面私有组件（仅本页用，不出子包）
        ├── role/index.vue
        └── …
```

规则：

1. `views` 第一级按模块（system）、第二级按业务域（user/role/menu/dept/dict/config/log），**与后端分包一一对应**，后端域目录名即前端页面目录名。
2. 页面私有组件放 `views/<域>/components/`，只有两个以上页面复用才上浮到 `src/components/` 并加 `App` 前缀。
3. `api/` 文件名 = 业务域名单数（user.ts 对应后端 user 域），**禁止**按 Controller 文件名命名。

## 2. 命名约定（强制）

| 对象 | 约定 | 示例 |
|------|------|------|
| 页面组件 | 业务域大驼峰 | `views/system/user/index.vue`（路由 name 用 `system-user`） |
| 全局公共组件 | `App` 前缀大驼峰 | `AppSearchForm.vue` |
| 组合式函数 | `use` 前缀小驼峰 | `useTable.ts` |
| 接口函数 | 动词 + 资源小驼峰 | `getUserPage` / `createUser` / `deleteUser` |
| TS 类型 | 与后端类名一致 | `UserVO` / `UserCreateDTO`（**后端叫什么前端就叫什么**，禁止二次发明） |
| 常量 | 全大写下划线 | `TOKEN_KEY` / `MENU_TYPE_BUTTON` |
| store | `use<域>Store` | `useAuthStore` / `useAppStore` |

## 3. 分层职责（强制）

1. **页面（views）只做三件事**：组装布局、绑定交互、消费 composables/api。禁止在页面里直接写 axios、直接拼 URL、直接处理分页状态。
2. **api 层**：每个函数 = 一个后端接口，入参出参必须标注契约类型，禁止 `any`/裸 `Promise<unknown>`。返回 `Result<T>` 由 request 封装解包，**api 函数直接返回 `T`**（业务失败已在拦截器 reject）。
3. **composables**：跨页面可复用的状态逻辑（分页状态机、表单弹窗开关）。一个 composable 只管一件事。
4. **types**：接口契约类型集中放 `types/api.d.ts`。生成优先（openapi-typescript），手工维护时必须与后端 record 字段逐一对齐。

## 4. 接口契约与类型（强制）

1. 后端字段名即前端字段名（camelCase 直传），**禁止**在前端做字段重命名映射。
2. 分页响应统一 `PageResult<T>`：`list/total/pageNum/pageSize/pages`（与后端 common.result.PageResult 对齐，注意是 **list** 不是 records）。
3. 时间字段后端输出 `yyyy-MM-dd HH:mm:ss` 字符串，前端按 string 接收，展示层不反序列化。
4. 字典/枚举值（status、menuType、dataScope 等）在 `constants/` 定义数值常量与文案映射，禁止在页面里裸写魔法数字。
5. 类型生成命令（后端启动后可执行，产物替换手写类型）：

   ```bash
   npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.d.ts
   ```

   引入代码生成前以手写类型为准；两者共存期间以手写为准，不允许混用两套类型。

## 5. 页面模板三件套（强制）

所有管理页（列表 + CRUD）使用统一骨架，差异只体现在列定义与表单字段：

1. **搜索区**：`AppSearchForm` 组件（v-model 收集查询参数 + 重置）。
2. **表格区**：`useTable()` 提供分页状态机（pageNum/pageSize/loading/records/total + reload），`a-table` 绑定之。
3. **表单弹窗**：`AppCrudModal` 组件（新增/编辑复用一个弹窗，`open(type, record?)` 驱动），提交调 api 成功后 `reload()`。

按钮级权限用 `v-perm` 指令：`v-perm="'system:user:create'"`，无权限直接移除 DOM；页面级数据权限由路由守卫控制。

## 6. 请求与认证约定（强制）

1. 所有请求走 `utils/request.ts` 单例，**禁止**在业务代码中 `import axios`。
2. 双 token 存 localStorage（`erp_access_token` / `erp_refresh_token`），由 `utils/token.ts` 读写，store 与拦截器都只调它。
3. 401 处理链：优先用 refresh token 无感刷新（并发请求合并等待同一刷新 promise）；刷新失败清 token 跳登录页。**禁止**在业务代码里自行处理 401。
4. 错误提示统一由拦截器 `message.error` 弹出（优先后端 message，兜底文案见 request.ts）；业务代码 catch 后**不得**再弹一次重复提示，除非需要定制交互。
5. GET 用 `request.get`，写操作一律 POST/PUT/DELETE 语义化方法，与后端 RESTful 路径一致，路径常量集中在各 api 文件头部。

## 7. TypeScript 硬约束（强制）

1. `strict: true`，**禁用 `any`**（确需 escape 时 `unknown` + 收窄，并注释原因）。
2. `noUnusedLocals/noUnusedParameters` 开启，编译期即拦截。
3. 模板事件/插槽的隐式 any 同样按违规处理（vue-tsc 会报）。
4. 类型导入用 `import type`（`verbatimModuleSyntax` 友好，也避免运行时误引入）。

## 8. 组件与样式约定

1. 一律 `<script setup lang="ts">` 组合式 API；逻辑顺序：类型/常量 → composable/状态 → 函数 → 生命周期。
2. 单文件 `.vue` 超过 **300 行**必须拆分（子组件/composable），列表页的列定义与表单 schema 允许抽到同目录 `config.ts`。
3. 样式：页面级样式用 `<style scoped>`；主题变量用 AntD token，禁止硬编码色值；间距遵循 8px 栅格。
4. 图标统一 `@ant-design/icons-vue`，菜单图标名与后端 `sys_menu.icon` 存储名一致。

## 9. 工程与提交门禁（强制）

1. `npm run build`（含 `vue-tsc -b`）零错误方可提交；后续引入 eslint 后增加 lint 门禁。
2. 禁止 `console.log` 提交进仓库（与后端 System.out.println 同级别禁令）。
3. commit Message 沿用 Angular 规范，scope 用 `frontend`，如 `feat(frontend): 用户管理页面`。
4. 开发代理：vite 已配置 `/api → http://localhost:8080`，前端代码中**不得**出现完整后端地址。

## 10. 页面开发自查清单

提交每个管理页前逐项确认：

- [ ] 使用了 AppSearchForm / useTable / AppCrudModal，无手写分页状态
- [ ] 所有 api 调用来自 `api/<域>.ts`，页面内无 axios、无 URL 字符串
- [ ] 类型来自 `types/api.d.ts`，无 any、无自定义字段别名
- [ ] 按钮挂 v-perm，权限点与后端 `sys_menu.perms` 一致
- [ ] 状态/枚举展示走 constants 映射，无裸数字
- [ ] 新增/编辑/删除成功后表格 reload，失败有后端 message
- [ ] `npm run build` 通过，文件未超 300 行
