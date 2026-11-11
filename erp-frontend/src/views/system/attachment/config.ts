// 附件管理页（F8）列定义与格式化工具：列名与后端 AttachmentVO 字段一一对应。
// 上传允许的扩展名对齐后端 AttachmentController 5.9.1 说明（pdf/jpg/jpeg/png/xlsx/docx，20MB 内）。
// 上传白名单/大小提示为附件域独有约束，保留域内不迁公共 constants。
import type { ColumnsType } from 'ant-design-vue/es/table'
import type { AttachmentVO, DictItemOptionVO } from '../../../types/api'

/** 附件业务类型字典类型编码（sys_dict_type.dict_type，业务类型下拉与表格翻译共用） */
export const BIZ_TYPE_DICT_TYPE = 'attachment_biz_type'

/** 由字典选项构建 itemValue -> itemLabel 映射（表格业务类型列翻译用，未命中回退原值） */
export function buildBizTypeLabelMap(options: DictItemOptionVO[]): Record<string, string> {
  const map: Record<string, string> = {}
  for (const option of options) {
    map[option.itemValue] = option.itemLabel
  }
  return map
}

/** 上传允许的文件扩展名（对齐后端 5.9.1 支持列表，类型不符后端 10401 拦截） */
export const UPLOAD_ACCEPT = '.pdf,.jpg,.jpeg,.png,.xlsx,.docx'

/** 单文件大小上限提示文案（后端 10402 拦截，前端仅提示不预校验） */
export const UPLOAD_SIZE_TIP = '单文件不超过 20MB，单单据不超过 20 个，支持 pdf/jpg/jpeg/png/xlsx/docx'

/** 文件大小格式化（后端 fileSize 为字节数字符串，展示为 B/KB/MB） */
export function formatFileSize(bytes: string | null): string {
  if (!bytes) {
    return '-'
  }
  const size = Number(bytes)
  if (Number.isNaN(size)) {
    return '-'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / (1024 * 1024)).toFixed(2)} MB`
}

/** 列定义（列表接口不分页，操作列含下载/删除） */
export const ATTACHMENT_COLUMNS: ColumnsType<AttachmentVO> = [
  { title: '文件名', dataIndex: 'fileName', width: 240, ellipsis: true },
  { title: '类型', dataIndex: 'fileType', width: 140, ellipsis: true },
  { title: '大小', dataIndex: 'fileSize', width: 100 },
  { title: '业务类型', dataIndex: 'bizType', width: 130, ellipsis: true },
  { title: '业务主键', dataIndex: 'bizId', width: 160, ellipsis: true },
  { title: '单据号', dataIndex: 'bizCode', width: 150, ellipsis: true },
  { title: '上传人', dataIndex: 'uploadBy', width: 110 },
  { title: '上传时间', dataIndex: 'uploadTime', width: 170 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' },
]
