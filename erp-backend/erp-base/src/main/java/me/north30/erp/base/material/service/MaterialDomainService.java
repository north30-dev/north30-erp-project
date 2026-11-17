package me.north30.erp.base.material.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import me.north30.erp.base.material.dto.MaterialCodeMapQueryDTO;
import me.north30.erp.base.material.dto.MaterialQueryDTO;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.entity.BaseMaterialCodeMap;

/**
 * 物料领域服务接口：承载本域业务规则（编码唯一、四要素重复、状态机、删除守卫）
 * 与本域 Mapper 网关（base_material / base_material_code_map）。
 * <p>领域服务只依赖本域 Mapper 与策略类，不注入任何跨域服务；
 * 跨域数据（分类编码/叶子校验）由应用服务层编排后作参数传入。</p>
 */
public interface MaterialDomainService {

    /**
     * 校验物料存在，不存在抛 11002（@TableLogic 自动过滤已删除行）。
     *
     * @param id 物料 ID
     * @return 物料实体
     */
    BaseMaterial requireMaterial(Long id);

    /**
     * 按编码校验物料存在，不存在抛 11002。
     *
     * @param materialCode 物料编码
     * @return 物料实体
     */
    BaseMaterial requireMaterialByCode(String materialCode);

    /**
     * 编码唯一性守卫（11001，新增/导入自定义编码场景）。
     *
     * @param materialCode 物料编码
     * @param excludeId    排除的物料 ID（修改传自身 ID，新增传 null）
     */
    void requireCodeAvailable(String materialCode, Long excludeId);

    /**
     * 四要素重复守卫（11008：名称+规格+品牌+单位）。
     *
     * @param materialName 物料名称
     * @param spec         规格型号（可空）
     * @param brand        品牌（可空）
     * @param unit         计量单位
     * @param excludeId    排除的物料 ID（修改传自身 ID，新增传 null）
     */
    void requireFourElementsAvailable(String materialName, String spec, String brand, String unit, Long excludeId);

    /**
     * 删除前置守卫（11011 引用计数 / 11009 非新建状态，顺序与文案收敛于状态机策略）。
     *
     * @param material       物料实体
     * @param referenceCount 业务单据引用计数（BOM/订单/流水；未落地前恒为 0）
     */
    void requireDeletable(BaseMaterial material, long referenceCount);

    /**
     * 统计指定分类下物料数（跨域最小接口，分类域删除校验 11006 用）。
     *
     * @param categoryId 分类 ID
     * @return 物料数
     */
    long countByCategoryId(Long categoryId);

    /**
     * 审核流转（0-新建 → 1-审核/驳回，非法前置抛 11010）并持久化。
     *
     * @param id       物料 ID
     * @param approved true-审核通过 / false-审核驳回
     * @return 变更后状态
     */
    Integer audit(Long id, boolean approved);

    /**
     * 启用流转（1-审核 → 2-启用，非法前置抛 11010/11003）并持久化。
     *
     * @param id 物料 ID
     * @return 变更后状态
     */
    Integer enable(Long id);

    /**
     * 停用流转（1-审核/2-启用 → 3-停用；停用原因落 remark）并持久化。
     *
     * @param id     物料 ID
     * @param reason 停用原因（可空）
     * @return 变更后状态
     */
    Integer disable(Long id, String reason);

    /**
     * 新增物料落库（主键由 MyBatis-Plus 雪花算法回填）。
     *
     * @param material 物料实体
     * @return 物料实体（回填主键）
     */
    BaseMaterial insert(BaseMaterial material);

    /**
     * 更新物料行，更新 0 行视为乐观锁冲突（10601，HTTP 409）。
     *
     * @param material 物料实体（version 须携带客户端版本号）
     */
    void updateRow(BaseMaterial material);

    /**
     * 按 ID 逻辑删除物料。
     *
     * @param id 物料 ID
     */
    void deleteById(Long id);

    /**
     * 批量按 ID 加载物料（@TableLogic 自动过滤已删除行）。
     *
     * @param ids 物料 ID 集合
     * @return 物料实体列表
     */
    List<BaseMaterial> listByIds(List<Long> ids);

    /**
     * 物料分页查询（创建时间倒序，分页参数归一收敛于查询策略）。
     *
     * @param query 查询参数
     * @return 物料分页结果
     */
    Page<BaseMaterial> pageMaterials(MaterialQueryDTO query);

    /**
     * 预载既有物料四要素索引（单次查询，导入场景内存查重，禁行级查库）。
     *
     * @return 四要素规范化键 → 既有物料编码
     */
    java.util.Map<String, String> loadFourElementIndex();

    /**
     * 物料列表查询（导出全量，创建时间倒序）。
     *
     * @param query 查询参数
     * @return 物料列表
     */
    List<BaseMaterial> listMaterials(MaterialQueryDTO query);

    /**
     * 旧码映射唯一性守卫（old_code + source_dept，11015）。
     *
     * @param oldCode    旧编码
     * @param sourceDept 来源部门
     * @param excludeId  排除的映射 ID（保留扩展，当前新增场景传 null）
     */
    void requireMappingAvailable(String oldCode, String sourceDept, Long excludeId);

    /**
     * 校验新物料存在并返回（不存在抛 11002，供回填 new_material_code）。
     *
     * @param newMaterialId 新物料 ID
     * @return 新物料实体
     */
    BaseMaterial requireNewMaterialExists(Long newMaterialId);

    /**
     * 新增旧码映射落库（主键由 MyBatis-Plus 雪花算法回填）。
     *
     * @param codeMap 映射实体
     * @return 映射实体（回填主键）
     */
    BaseMaterialCodeMap insertCodeMap(BaseMaterialCodeMap codeMap);

    /**
     * 旧码映射分页查询（迁移时间倒序）。
     *
     * @param query 查询参数
     * @return 旧码映射分页结果
     */
    Page<BaseMaterialCodeMap> pageCodeMaps(MaterialCodeMapQueryDTO query);
}
