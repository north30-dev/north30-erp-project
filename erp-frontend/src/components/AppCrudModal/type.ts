/** AppCrudModal 对外类型 */
import type { Rule } from 'ant-design-vue/es/form'

/** 弹窗模式：新增 / 编辑 */
export type CrudMode = 'create' | 'edit'

/** 表单对象（键与插槽内 a-form-item 的 name 对齐） */
export type CrudForm = Record<string, unknown>

/** 校验规则表（键同上） */
export type CrudRules = Record<string, Rule[]>

/** 提交回调：resolve 关闭弹窗，reject 保持打开（错误提示由 request 拦截器统一弹出） */
export type CrudSubmit = (form: CrudForm) => Promise<void>

/** 编辑回填来源（open('edit', record) 时浅拷贝进表单） */
export type CrudRecord = Record<string, unknown> | undefined
