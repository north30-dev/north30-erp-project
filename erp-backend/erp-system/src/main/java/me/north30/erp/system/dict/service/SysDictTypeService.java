package me.north30.erp.system.dict.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.dict.dto.DictTypeCreateDTO;
import me.north30.erp.system.dict.dto.DictTypeQueryDTO;
import me.north30.erp.system.dict.dto.DictTypeUpdateDTO;
import me.north30.erp.system.dict.entity.SysDictType;
import me.north30.erp.system.dict.vo.DictTypeVO;
import me.north30.erp.system.common.vo.MutationVO;

/**
 * 字典类型管理服务接口（接口文档 5.5.1-5.5.4，33-36 号接口）。
 */
public interface SysDictTypeService {

    /**
     * 字典类型分页查询，携带字典项数量（一条分组统计，禁 N+1）。
     * 
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<DictTypeVO> page(DictTypeQueryDTO query);

    /**
     * 按类型编码查询字典类型，不存在抛业务异常（供跨域/本域复用）。
     *
     * @param dictType 字典类型编码
     * @return 字典类型实体
     */
    SysDictType requireByDictType(String dictType);

    /**
     * 新增字典类型：编码格式与唯一性校验。
     * 
     * @param dto 创建参数
     * @return 操作结果
     */
    MutationVO create(DictTypeCreateDTO dto);

    /**
     * 修改字典类型：dict_type 不可修改，乐观锁控制并发。
     * 
     * @param id 字典类型 ID
     * @param dto 更新参数
     * @return 操作结果
     */
    MutationVO update(Long id, DictTypeUpdateDTO dto);

    /**
     * 删除字典类型（逻辑删除）：类型下存在字典项不可删。
     * 
     * @param id 字典类型 ID
     * @return 操作结果
     */
    MutationVO delete(Long id);
}
