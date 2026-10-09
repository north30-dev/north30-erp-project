package me.north30.erp.system.dict.service;

import java.util.List;
import java.util.Map;

/**
 * 字典项基础服务接口（领域基础服务：只读 + 守卫，供编排层/跨域消费）。
 * <p>管理端 8 个用例见 {@link DictManagementService}；本接口只依赖本域 Mapper，
 * 不反向依赖编排层，保证依赖无环。</p>
 */
public interface SysDictItemService {

    /**
     * 按类型编码批量统计字典项数量（一条 group by 查询，禁 N+1，供类型分页/删除守卫复用）。
     *
     * @param dictTypes 字典类型编码列表
     * @return 类型编码 → 字典项数量
     */
    Map<String, Long> countByTypes(List<String> dictTypes);
}
