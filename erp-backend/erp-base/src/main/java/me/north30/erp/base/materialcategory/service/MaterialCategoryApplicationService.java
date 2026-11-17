package me.north30.erp.base.materialcategory.service;

import java.util.List;

import me.north30.erp.base.materialcategory.dto.CategoryCreateDTO;
import me.north30.erp.base.materialcategory.dto.CategoryMoveDTO;
import me.north30.erp.base.materialcategory.dto.CategoryStatusDTO;
import me.north30.erp.base.materialcategory.dto.CategoryTreeQueryDTO;
import me.north30.erp.base.materialcategory.dto.CategoryUpdateDTO;
import me.north30.erp.base.materialcategory.vo.CategoryMutationVO;
import me.north30.erp.base.materialcategory.vo.CategoryStatusVO;
import me.north30.erp.base.materialcategory.vo.CategoryTreeVO;

/**
 * 物料分类应用服务接口：事务编排层（写操作事务边界 + 跨层/跨域调用编排），
 * 不写业务规则、不注入 Mapper。
 * <p>跨域依赖（物料域挂载物料计数）在本层取得后作参数传入 DomainService；
 * 物料域校验 11004/11005/11017 直接经 {@code MaterialCategoryDomainService} 最小接口，不经本接口。</p>
 */
public interface MaterialCategoryApplicationService {

    /**
     * 物料分类树查询（一次查全量后内存组树，禁 N+1）。
     *
     * @param query 查询参数（名称/编码模糊、状态）
     * @return 两级分类树
     */
    List<CategoryTreeVO> listTree(CategoryTreeQueryDTO query);

    /**
     * 新增物料分类：编码唯一（11007）、层级上限 2 级（10001）、编码联动（10001），
     * 新增二级分类后刷新父级叶子标记。
     *
     * @param dto 新增参数
     * @return 新分类 ID、层级与叶子标记
     */
    CategoryMutationVO create(CategoryCreateDTO dto);

    /**
     * 修改物料分类：编码与父级不可修改，乐观锁冲突返回 409。
     *
     * @param id  分类 ID
     * @param dto 修改参数（选填字段传 null 表示不修改）
     * @return 分类 ID、层级与叶子标记、更新时间
     */
    CategoryMutationVO update(Long id, CategoryUpdateDTO dto);

    /**
     * 删除物料分类（逻辑删除）：存在下级分类或已挂载物料抛 11006；
     * 删除末个二级分类后刷新父级叶子标记。
     *
     * @param id 分类 ID
     * @return 分类 ID 与逻辑删除标记
     */
    CategoryMutationVO delete(Long id);

    /**
     * 变更分类状态（启用/停用）：乐观锁冲突返回 409；停用分类不可挂载物料由物料域落地（11017）。
     *
     * @param id  分类 ID
     * @param dto 目标状态与乐观锁版本号
     * @return 变更后的状态
     */
    CategoryStatusVO changeStatus(Long id, CategoryStatusDTO dto);

    /**
     * 同级上移/下移：规范化重排（10,20,30…）后相邻交换，乐观锁冲突返回 409，
     * 已处于顶部/底部抛 10001。
     *
     * @param id  分类 ID
     * @param dto 移动方向 up/down
     */
    void move(Long id, CategoryMoveDTO dto);
}
