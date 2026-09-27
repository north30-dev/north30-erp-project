package me.north30.erp.system.core.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.core.dto.DictTypeCreateDTO;
import me.north30.erp.system.core.dto.DictTypeQueryDTO;
import me.north30.erp.system.core.dto.DictTypeUpdateDTO;
import me.north30.erp.system.core.vo.DictTypeVO;
import me.north30.erp.system.core.vo.MutationVO;

/**
 * 字典类型管理服务接口（接口文档 5.5.1-5.5.4，33-36 号接口）。
 */
public interface ISysDictTypeService {

    /**
     * 字典类型分页查询，携带字典项数量（一条分组统计，禁 N+1）。
     */
    PageResult<DictTypeVO> page(DictTypeQueryDTO query);

    /**
     * 新增字典类型：编码格式与唯一性校验。
     */
    MutationVO create(DictTypeCreateDTO dto);

    /**
     * 修改字典类型：dict_type 不可修改，乐观锁控制并发。
     */
    MutationVO update(Long id, DictTypeUpdateDTO dto);

    /**
     * 删除字典类型（逻辑删除）：类型下存在字典项不可删。
     */
    MutationVO delete(Long id);
}
