package me.north30.erp.system.dict.service;

import me.north30.erp.system.dict.entity.SysDictType;

/**
 * 字典类型基础服务接口（领域基础服务：只读 + 守卫，供编排层/跨域消费）。
 * <p>管理端 8 个用例见 {@link DictManagementService}；本接口只依赖本域 Mapper，
 * 不反向依赖编排层，保证依赖无环。</p>
 */
public interface SysDictTypeService {

    /**
     * 按类型编码查询字典类型，不存在抛业务异常（供跨域/本域复用）。
     *
     * @param dictType 字典类型编码
     * @return 字典类型实体
     */
    SysDictType requireByDictType(String dictType);
}
