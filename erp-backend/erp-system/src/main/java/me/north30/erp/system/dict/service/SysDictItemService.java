package me.north30.erp.system.dict.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.dict.dto.DictItemCreateDTO;
import me.north30.erp.system.dict.dto.DictItemQueryDTO;
import me.north30.erp.system.dict.dto.DictItemUpdateDTO;
import me.north30.erp.system.dict.vo.DictItemVO;
import me.north30.erp.system.common.vo.MutationVO;

/**
 * 字典项管理服务接口（接口文档 5.5.5-5.5.8，37-40 号接口）。
 */
public interface SysDictItemService {

    /**
     * 字典项分页查询（按字典类型过滤，lang 默认 zh-CN）。
     */
    PageResult<DictItemVO> page(DictItemQueryDTO query);

    /**
     * 新增字典项：所属类型须存在，dict_type+item_value+lang 唯一。
     */
    MutationVO create(DictItemCreateDTO dto);

    /**
     * 修改字典项：item_value 不可修改，乐观锁控制并发。
     */
    MutationVO update(Long id, DictItemUpdateDTO dto);

    /**
     * 删除字典项（逻辑删除）。
     */
    MutationVO delete(Long id);
}
