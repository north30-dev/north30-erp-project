import { get } from '../utils/request'
import type { ImportResultVO, ImportTaskVO, PageQuery, PageResult } from '../types/api'

/**
 * 批量导入任务接口（对应后端 ImportTaskController /api/system/imports，接口文档 5.10）。
 * D2 仅查询能力：任务列表为占位实现（无 sys_import_task 表，恒返回空分页）；结果查询按任务不存在（10503）处理；
 * 导入执行（POST）由后续业务模块落地，届时本文件补充对应函数。
 */

/** 导入任务分页查询（权限点 system:import:detail；后端无查询 DTO，仅 pageNum/pageSize 两个 @RequestParam） */
export function getImportTaskPage(page: PageQuery): Promise<PageResult<ImportTaskVO>> {
  return get<PageResult<ImportTaskVO>>('/system/imports', page as Record<string, unknown>).then((r) => r.data)
}

/** 导入结果查询（5.10.2，权限点 system:import:detail；含错误行明细；任务号为空 10001/任务不存在 10503） */
export function getImportResult(taskId: string): Promise<ImportResultVO> {
  return get<ImportResultVO>(`/system/imports/${taskId}/result`).then((r) => r.data)
}
