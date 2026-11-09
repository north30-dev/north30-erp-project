// 单据编号序列页（F8）列定义与校验常量：列名与后端 CodeSequenceVO 字段一一对应。
// 期间格式与后端 CodeSequenceResetDTO @Pattern 一致（yyyyMM 六位数字），前端查询/弹窗共用。
import type { ColumnsType } from 'ant-design-vue/es/table'
import type { CodeSequenceVO } from '../../../types/api'

/** 期间格式（与后端 CodeSequenceResetDTO @Pattern 一致：yyyyMM 六位数字） */
export const PERIOD_PATTERN = /^\d{6}$/

/** 列定义（重置动作走行内按钮） */
export const CODE_SEQUENCE_COLUMNS: ColumnsType<CodeSequenceVO> = [
  { title: '单据类型', dataIndex: 'bizType', width: 170, ellipsis: true },
  { title: '编号前缀', dataIndex: 'prefix', width: 120 },
  { title: '期间', dataIndex: 'period', width: 110 },
  { title: '当前流水号', dataIndex: 'currentNo', width: 120 },
  { title: '流水位数', dataIndex: 'seqLength', width: 100 },
  { title: '下一编号预览', dataIndex: 'nextCodePreview', width: 220, ellipsis: true },
  { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' },
]
