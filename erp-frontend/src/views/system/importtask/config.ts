// 导入任务页（F8）列定义：任务列表列名与后端 ImportTaskVO 字段一一对应，错误行列名与 ImportErrorVO 对应。
// D2 后端为占位实现（任务列表恒返回空分页），列结构按契约先行，导入落地后无需改动。
import type { ColumnsType } from 'ant-design-vue/es/table'
import type { ImportErrorVO, ImportTaskVO } from '../../../types/api'

/** 列定义（D2 占位实现恒空分页，操作列为"结果"按钮） */
export const IMPORT_TASK_COLUMNS: ColumnsType<ImportTaskVO> = [
  { title: '任务 ID', dataIndex: 'taskId', width: 200, ellipsis: true },
  { title: '业务类型', dataIndex: 'bizType', width: 150, ellipsis: true },
  { title: '导入批次', dataIndex: 'importBatch', width: 180, ellipsis: true },
  { title: '总行数', dataIndex: 'totalRows', width: 90 },
  { title: '成功行数', dataIndex: 'successRows', width: 100 },
  { title: '失败行数', dataIndex: 'failRows', width: 100 },
  { title: '已提交', dataIndex: 'committed', width: 90 },
  { title: '操作', key: 'action', width: 90, fixed: 'right' },
]

/** 错误行列定义（结果抽屉内，对齐后端 ImportErrorVO 字段；rawData 为脱敏后的原始行数据） */
export const IMPORT_ERROR_COLUMNS: ColumnsType<ImportErrorVO> = [
  { title: 'Excel 行号', dataIndex: 'rowNo', width: 100 },
  { title: '出错字段', dataIndex: 'fieldName', width: 140, ellipsis: true },
  { title: '错误码', dataIndex: 'errorCode', width: 90 },
  { title: '错误信息', dataIndex: 'errorMessage', width: 240, ellipsis: true },
  { title: '原始行数据', dataIndex: 'rawData', width: 220, ellipsis: true },
]
