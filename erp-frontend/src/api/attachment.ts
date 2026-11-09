import { del, get, getBlob, post } from '../utils/request'
import type { AttachmentBizQueryDTO, AttachmentVO, DeleteResultVO } from '../types/api'

/**
 * 附件管理接口（对应后端 AttachmentController /api/system/attachments，接口文档 5.9.1-5.9.4）。
 * 上传为 multipart form：file 文件 + bizType/bizId/bizCode 业务参数（后端 @RequestPart + @ParameterObject 绑定）；
 * 列表按业务类型 + 业务主键查询且不分页（后端返回 List）。
 */

/** 附件上传（5.9.1，权限点 system:attachment:upload；单文件 ≤20MB，单单据 ≤20 个，支持 pdf/jpg/jpeg/png/xlsx/docx；类型不支持 10401/超限 10402/10403） */
export function uploadAttachment(file: File, data: AttachmentBizQueryDTO): Promise<AttachmentVO> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', data.bizType)
  formData.append('bizId', data.bizId)
  if (data.bizCode) {
    formData.append('bizCode', data.bizCode)
  }
  return post<AttachmentVO>('/system/attachments', formData).then((r) => r.data)
}

/** 附件列表查询（5.9.3，权限点 system:attachment:list；按业务类型 + 业务主键，不分页，按 ID 倒序） */
export function getAttachmentList(query: AttachmentBizQueryDTO): Promise<AttachmentVO[]> {
  // AttachmentBizQueryDTO 含必填字段（区别于全可选的各域分页 QueryDTO），转 Record 需经 unknown 二段断言
  return get<AttachmentVO[]>('/system/attachments', query as unknown as Record<string, unknown>).then((r) => r.data)
}

/** 附件下载（5.9.2，权限点 system:attachment:download；返回文件流，附件不存在 18038；文件名由调用方以行数据 fileName 命名） */
export function downloadAttachment(id: string): Promise<Blob> {
  return getBlob(`/system/attachments/${id}/download`)
}

/** 附件删除（5.9.4，权限点 system:attachment:delete；逻辑删除，仅上传人可删除，10202 无权操作） */
export function deleteAttachment(id: string): Promise<DeleteResultVO> {
  return del<DeleteResultVO>(`/system/attachments/${id}`).then((r) => r.data)
}
