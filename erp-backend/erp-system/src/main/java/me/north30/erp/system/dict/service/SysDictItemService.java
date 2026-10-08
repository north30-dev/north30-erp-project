package me.north30.erp.system.dict.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.dict.dto.DictItemCreateDTO;
import me.north30.erp.system.dict.dto.DictItemQueryDTO;
import me.north30.erp.system.dict.dto.DictItemUpdateDTO;
import me.north30.erp.system.dict.vo.DictItemVO;
import me.north30.erp.system.common.vo.MutationVO;

import java.util.List;
import java.util.Map;

/**
 * 字典项管理服务接口（接口文档 5.5.5-5.5.8，37-40 号接口）。
 */
public interface SysDictItemService {

    /**
     * 字典项分页查询（按字典类型过滤，lang 默认 zh-CN）。
     * 
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<DictItemVO> page(DictItemQueryDTO query);

    /**
     * 按类型编码批量统计字典项数量（一条 group by 查询，禁 N+1，供类型分页复用）。
     *
     * @param dictTypes 字典类型编码列表
     * @return 类型编码 → 字典项数量
     */
    Map<String, Long> countByTypes(List<String> dictTypes);

    /**
     * 新增字典项：所属类型须存在，dict_type+item_value+lang 唯一。
     * 
     * @param dto 创建参数
     * @return 操作结果
     */
    MutationVO create(DictItemCreateDTO dto);

    /**
     * 修改字典项：item_value 不可修改，乐观锁控制并发。
     * 
     * @param id 字典项 ID
     * @param dto 更新参数
     * @return 操作结果
     */
    MutationVO update(Long id, DictItemUpdateDTO dto);

    /**
     * 删除字典项（逻辑删除）。
     * 
     * @param id 字典项 ID
     * @return 操作结果
     */
    MutationVO delete(Long id);
}
