/** 后端统一响应体（与后端 me.north30.erp.common.result.Result 对应） */
export interface Result<T = unknown> {
  /** 业务状态码，200=成功 */
  code: number;
  /** 提示信息 */
  message: string;
  /** 业务数据 */
  data: T;
  /** 响应时间戳（毫秒） */
  timestamp: number;
  /** 链路追踪 ID */
  traceId?: string;
}

/** 分页结果（与后端 PageResult 对应） */
export interface PageResult<T> {
  /** 总记录数 */
  total: number;
  /** 当前页码 */
  pageNum: number;
  /** 每页条数 */
  pageSize: number;
  /** 当前页数据 */
  records: T[];
}
