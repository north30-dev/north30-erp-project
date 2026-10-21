import { ref } from 'vue'
import type { Ref } from 'vue'
import { DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE } from '../constants'
import type { PageQuery, PageResult } from '../types/api'

/**
 * 列表页分页状态机（编码规范第 5 节：管理页统一消费本函数，禁止手写分页状态）。
 *
 * @param fetcher 分页查询函数：接收合并后的查询参数，返回分页结果
 * @param initialQuery 初始查询条件（表单字段）
 * @example
 * const { draftQuery, records, total, loading, search, reset, reload, handleTableChange }
 *   = useTable((q) => getUserPage(q), { username: '', status: undefined })
 */
export function useTable<Q extends object, T = unknown>(
  fetcher: (query: Q & PageQuery) => Promise<PageResult<T>>,
  initialQuery: Q,
) {
  /** 查询条件 + 分页参数（a-table 直接绑定） */
  const pageNum = ref(DEFAULT_PAGE_NUM)
  const pageSize = ref(DEFAULT_PAGE_SIZE)
  /** 查询条件（已提交生效的版本；表单编辑用 draftQuery，点查询后提交） */
  // as Ref<Q>：绕开 reactive 解包类型 UnwrapRef<Q> 与泛型 Q 的不兼容（Q 仅作数据载体，不解包）
  const query = ref<Q>({ ...initialQuery }) as Ref<Q>
  const draftQuery = ref<Q>({ ...initialQuery }) as Ref<Q>

  const records = ref<T[]>([])
  const total = ref(0)
  const loading = ref(false)

  async function load(): Promise<void> {
    loading.value = true
    try {
      const page = await fetcher({ ...query.value, pageNum: pageNum.value, pageSize: pageSize.value })
      records.value = page.list
      total.value = page.total
    } finally {
      loading.value = false
    }
  }

  /** 点"查询"按钮：提交草稿条件、重置页码到第 1 页后加载 */
  async function search(): Promise<void> {
    query.value = { ...draftQuery.value }
    pageNum.value = DEFAULT_PAGE_NUM
    await load()
  }

  /** 点"重置"按钮：清空条件、回到第 1 页后加载 */
  async function reset(): Promise<void> {
    draftQuery.value = { ...initialQuery }
    query.value = { ...initialQuery }
    pageNum.value = DEFAULT_PAGE_NUM
    await load()
  }

  /** 增删改成功后的表格刷新（保持当前页码） */
  async function reload(): Promise<void> {
    await load()
  }

  /** a-table change 事件：分页/排序变化 */
  async function handleTableChange(page: { current?: number; pageSize?: number }): Promise<void> {
    if (page.current) {
      pageNum.value = page.current
    }
    if (page.pageSize) {
      pageSize.value = page.pageSize
    }
    await load()
  }

  return { query, draftQuery, pageNum, pageSize, records, total, loading, search, reset, reload, handleTableChange }
}
