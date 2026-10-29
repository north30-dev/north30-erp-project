package me.north30.erp.system.dict.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.common.vo.MutationVO;
import me.north30.erp.system.dict.dto.DictTypeCreateDTO;
import me.north30.erp.system.dict.dto.DictTypeQueryDTO;
import me.north30.erp.system.dict.dto.DictTypeStatusDTO;
import me.north30.erp.system.dict.dto.DictTypeUpdateDTO;
import me.north30.erp.system.dict.entity.SysDictType;
import me.north30.erp.system.dict.vo.DictTypeStatusVO;
import me.north30.erp.system.dict.vo.DictTypeVO;

/**
 * 字典类型服务（A 类单层：4 个管理端用例 + 守卫，接口文档 5.5.1-5.5.4，33-36 号接口）。
 * <p>只依赖本域 Mapper（含字典项 Mapper 做计数守卫），不依赖字典项 Service，保证无循环依赖。</p>
 */
public interface SysDictTypeService {

    /**
     * 字典类型分页查询，携带字典项数量（一条分组统计，禁 N+1，5.5.1）。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<DictTypeVO> pageType(DictTypeQueryDTO query);

    /**
     * 新增字典类型：编码唯一性校验，未指定 status 默认启用（5.5.2）。
     *
     * @param dto 创建参数
     * @return 操作结果
     */
    MutationVO createType(DictTypeCreateDTO dto);

    /**
     * 修改字典类型：dict_type 不可修改，乐观锁控制并发（5.5.3）。
     *
     * @param id  字典类型 ID
     * @param dto 更新参数
     * @return 操作结果
     */
    MutationVO updateType(Long id, DictTypeUpdateDTO dto);

    /**
     * 删除字典类型（逻辑删除）：类型下存在字典项不可删（5.5.4）。
     *
     * @param id 字典类型 ID
     * @return 删除结果
     */
    DeleteResultVO deleteType(Long id);

    /**
     * 变更字典类型状态（启用/停用，08 文档阶段四状态开关统一）。
     *
     * @param id  字典类型 ID
     * @param dto 状态变更参数
     * @return 状态变更结果
     */
    DictTypeStatusVO changeStatusType(Long id, DictTypeStatusDTO dto);

    /**
     * 校验字典类型编码可用（未被占用），已存在抛 18029 业务异常。
     *
     * @param dictType 字典类型编码
     */
    void requireDictTypeAvailable(String dictType);

    /**
     * 按 ID 查询字典类型，不存在抛 18028 业务异常（供本域/跨域复用）。
     *
     * @param id 字典类型 ID
     * @return 字典类型实体
     */
    SysDictType requireById(Long id);

    /**
     * 按类型编码查询字典类型，不存在抛 18028 业务异常（供本域/跨域复用）。
     *
     * @param dictType 字典类型编码
     * @return 字典类型实体
     */
    SysDictType requireByDictType(String dictType);
}
