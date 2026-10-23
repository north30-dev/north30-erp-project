# ERP系统开发组件库使用规范文档

| 文档属性 | 内容 |
|---------|------|
| 文档名称 | ERP系统开发组件库使用规范文档 |
| 版本号 | V1.0 |
| 状态 | 生效中 |
| 适用范围 | erp-frontend 全部页面与组件 |
| 上位文档 | ERP系统开发前端编码规范文档 第 8 节 |

本规范的目的：统一 UI 实现方式，杜绝"每种写法并存"的样式债。核心原则一句话——**能用组件库解决的事，绝不平铺 HTML + 手写 CSS**。

---

## 1. 组件库选型与查询（强制）

1. UI 组件统一使用 **Ant Design Vue 4.x**（已全量注册，`main.ts` 中 `app.use(Antd)`，模板中直接写 `<a-xxx>`，无需逐个 import）。
2. 图标统一使用 **@ant-design/icons-vue**，禁止引入其他图标库。
3. **AI 编码代理在写任何页面前，必须先通过 antd 技能（`Use Skill: antd`）查询组件**：确认组件存在、确认 props/slots/events 的正确用法，禁止凭记忆杜撰 API。典型查询场景：
   - 不确定某个交互该用什么组件（如树形选择、穿梭框、抽屉）→ `antd_info` / `antd_list` 查全量组件清单；
   - 需要某组件的具体用法与示例 → `antd_demo`；
   - 需要确认某 props 的事件签名或语义 token → `antd_doc` / `antd_token`。
4. 优先用现成组件组合实现交互；只有当组件库确实没有对应能力时（先查询确认），才允许自研，且自研部分必须收敛为公共组件（`App` 前缀）。

## 2. 样式铁律（强制）

1. **非必要不写样式**：布局、间距、颜色、状态反馈一律优先用组件库的 props / 预设能力实现（如 `a-space`、`a-row`/`a-col`、`a-form layout`、`a-typography`、`a-divider`、`a-card`）。
2. 以下情况**禁止**手写 CSS：
   - 用 `a-space`、`a-flex`、`a-row`/`a-col` 能解决的排列；
   - 用组件 props 能表达的视觉差异（`type`/`size`/`ghost`/`danger`/`status` 等）；
   - 用 `a-typography-text type="secondary"` 能表达的弱化文案。
3. 允许写 `<style scoped>` 的**唯一场景**：组件库 props 无法表达的页面级微调（如登录页居中布局、验证码图片尺寸）。且必须遵守：
   - 颜色、间距、圆角一律使用 AntD theme token 值（如 `#1890ff` 这类魔法色值禁止，改用 `token.colorPrimary` 或 CSS 变量），禁止硬编码；
   - 单文件样式块 ≤ 30 行，超出说明该抽象成组件或改用布局组件；
   - 禁止 `!important`、禁止深度选择器穿透第三方组件内部结构（`:deep()` 仅限 AppSearchForm 这类自研公共组件）。
4. 主题定制只允许走 AntD 的 ConfigProvider theme token 机制，禁止覆盖 `.ant-xxx` 类名。

## 3. 图标规则（强制）

1. **严禁使用 emoji 代替组件库图标**（表格操作列、按钮、空状态、消息提示中出现 👍✏️🗑️❌ 等一律视为违规）。
2. 图标一律从 `@ant-design/icons-vue` 具名导入，如 `PlusOutlined`、`EditOutlined`、`DeleteOutlined`、`SearchOutlined`、`ReloadOutlined`。
3. 常用映射（后续页面照此复用，不重新发明）：
   - 新增 `PlusOutlined` / 编辑 `EditOutlined` / 删除 `DeleteOutlined` / 查询 `SearchOutlined`
   - 重置 `ReloadOutlined` / 详情 `EyeOutlined` / 导出 `ExportOutlined` / 下载 `DownloadOutlined`
   - 成功提示 `CheckCircleOutlined` / 警告 `WarningOutlined` / 用户 `UserOutlined` / 菜单 `MenuOutlined`
4. 业务图标名与后端 `sys_menu.icon` 存储名保持一致（前端编码规范第 8 节第 4 条）。

## 4. 交互组件选用约定（强制）

统一交互 → 统一组件，禁止同场景多种实现并存：

| 场景 | 统一用 | 禁止 |
|------|--------|------|
| 消息提示 | `message`（轻提示）/ `Modal.confirm`（危险确认） | alert、自研弹层 |
| 删除等危险操作确认 | `Modal.confirm`（危险按钮 `danger`） | 原生 confirm |
| 表单校验错误 | a-form rules + 校验状态 | message 报错代替行内校验 |
| 加载态 | 按钮 `loading` / 表格 `loading` / `a-spin` | 自研遮罩 |
| 空数据 | `a-table` 内置 Empty（不额外定制） | 手写"暂无数据"div |
| 页面跳转/返回 | `a-button` + router | a 标签裸跳转（页内操作列链接除外） |
| 树形选择 | `a-tree-select` | 级联下拉拼装 |
| 详情展示 | `a-descriptions` | 手写 dl/table 布局 |
| 数值/状态标签 | `a-tag`（配预设 color） | 彩色 span |
| 分页表格 | `a-table` + useTable | 自研分页条 |

## 5. AI 代理工作流（强制）

写页面时按以下顺序自检：

1. 交互清单 → 逐项 `Use Skill: antd` 查询对应组件与 API；
2. 布局只用组件库布局件 + token；
3. 图标全部来自 icons-vue，grep 检查无 emoji；
4. `<style scoped>` 仅剩布局微调且无魔法色值；
5. `npm run build` 通过。

提交前 grep 自查（命中即违规）：

```bash
grep -rn "style=" src/views/ | grep -v ":"
grep -rPn "[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}]" src/
grep -rn "#[0-9a-fA-F]\{6\}" src/views/ src/components/
```
