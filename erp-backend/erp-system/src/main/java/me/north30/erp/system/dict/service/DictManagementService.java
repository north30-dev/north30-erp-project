package me.north30.erp.system.dict.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.dict.dto.DictItemCreateDTO;
import me.north30.erp.system.dict.dto.DictItemQueryDTO;
import me.north30.erp.system.dict.dto.DictItemUpdateDTO;
import me.north30.erp.system.dict.dto.DictTypeCreateDTO;
import me.north30.erp.system.dict.dto.DictTypeQueryDTO;
import me.north30.erp.system.dict.dto.DictTypeUpdateDTO;
import me.north30.erp.system.dict.vo.DictItemVO;
import me.north30.erp.system.dict.vo.DictTypeVO;
import me.north30.erp.system.common.vo.MutationVO;

/**
 * 数据字典管理服务接口（字典类型 + 字典项 8 个管理端用例，接口文档 5.5.1-5.5.8，33-40 号接口）。
 * <p>本域唯一编排层：依赖方向为 Management → 基础 Service（{@code SysDictTypeService} /
 * {@code SysDictItemService}）→ Mapper，基础 Service 不反向依赖本类，保证无循环依赖。</p>
 */
public interface DictManagementService {

    /**
     * 字典类型分页查询，携带字典项数量（一条分组统计，禁 N+1，5.5.1）。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<DictTypeVO> pageType(DictTypeQueryDTO query);

    /**
     * 新增字典类型：编码格式与唯一性校验（5.5.2）。
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
     * @return 操作结果
     */
    MutationVO deleteType(Long id);

    /**
     * 字典项分页查询（按字典类型过滤，lang 默认 zh-CN，5.5.5）。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<DictItemVO> pageItem(DictItemQueryDTO query);

    /**
     * 新增字典项：所属类型须存在，dict_type + item_value + lang 唯一（5.5.6）。
     *
     * @param dto 创建参数
     * @return 操作结果
     */
    MutationVO createItem(DictItemCreateDTO dto);

    /**
     * 修改字典项：item_value 不可修改，乐观锁控制并发（5.5.7）。
     *
     * @param id  字典项 ID
     * @param dto 更新参数
     * @return 操作结果
     */
    MutationVO updateItem(Long id, DictItemUpdateDTO dto);

    /**
     * 删除字典项（逻辑删除，5.5.8）。
     *
     * @param id 字典项 ID
     * @return 操作结果
     */
    MutationVO deleteItem(Long id);
}
