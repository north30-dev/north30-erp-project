package me.north30.erp.system.dict.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.common.vo.MutationVO;
import me.north30.erp.system.dict.dto.DictItemCreateDTO;
import me.north30.erp.system.dict.dto.DictItemMoveDTO;
import me.north30.erp.system.dict.dto.DictItemQueryDTO;
import me.north30.erp.system.dict.dto.DictItemStatusDTO;
import me.north30.erp.system.dict.dto.DictItemUpdateDTO;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.vo.DictItemStatusVO;
import me.north30.erp.system.dict.vo.DictItemVO;

/**
 * 字典项服务（A 类单层：4 个管理端用例 + 守卫，接口文档 5.5.5-5.5.8，37-40 号接口）。
 * <p>只依赖本域 Mapper（含字典类型 Mapper 做类型存在性守卫），不依赖字典类型 Service，保证无循环依赖。</p>
 */
public interface SysDictItemService {

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
     * @return 删除结果
     */
    DeleteResultVO deleteItem(Long id);

    /**
     * 变更字典项状态（启用/停用，08 文档阶段四状态开关统一）。
     *
     * @param id  字典项 ID
     * @param dto 状态变更参数
     * @return 状态变更结果
     */
    DictItemStatusVO changeStatusItem(Long id, DictItemStatusDTO dto);

    /**
     * 字典项上移/下移（同 dict_type + lang 分组内规范化重排 10,20,30… 后相邻交换，
     * 08 文档阶段四排序统一）。
     *
     * @param id  字典项 ID
     * @param dto 移动方向参数
     */
    void moveItem(Long id, DictItemMoveDTO dto);

    /**
     * 校验字典项可用（同 dict_type + item_value + lang 未被占用），已存在抛 18031 业务异常。
     *
     * @param dictType  字典类型编码
     * @param itemValue 字典项值
     * @param lang      语言
     */
    void requireItemAvailable(String dictType, String itemValue, String lang);

    /**
     * 按 ID 查询字典项，不存在抛 10004 业务异常（供本域/跨域复用）。
     *
     * @param id 字典项 ID
     * @return 字典项实体
     */
    SysDictItem requireById(Long id);
}
