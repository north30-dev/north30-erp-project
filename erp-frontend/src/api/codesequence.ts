import { get, post } from '../utils/request'
import type {
  CodeSequenceQueryDTO,
  CodeSequenceResetDTO,
  CodeSequenceResetVO,
  CodeSequenceVO,
  PageResult,
} from '../types/api'

/**
 * 单据编号序列接口（对应后端 SysCodeSequenceController /api/system/code-sequences，接口文档 5.7，45-46 号接口）。
 * 序列为只读展示 + 流水重置两类操作，无新增/删除接口（序列随单据生成自动建立）。
 */

/** 编号序列分页查询（5.7.1，权限点 system:sequence:list） */
export function getCodeSequencePage(query: CodeSequenceQueryDTO): Promise<PageResult<CodeSequenceVO>> {
  return get<PageResult<CodeSequenceVO>>('/system/code-sequences', query as Record<string, unknown>).then((r) => r.data)
}

/** 重置编号流水（5.7.2，权限点 system:sequence:reset；仅允许向下重置至未使用区间，目标值相同幂等跳过；序列不存在 18035/重置值冲突 18036/乐观锁冲突 10601） */
export function resetCodeSequence(data: CodeSequenceResetDTO): Promise<CodeSequenceResetVO> {
  return post<CodeSequenceResetVO>('/system/code-sequences/reset', data).then((r) => r.data)
}
