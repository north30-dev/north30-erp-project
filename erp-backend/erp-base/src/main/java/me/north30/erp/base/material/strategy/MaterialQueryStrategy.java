package me.north30.erp.base.material.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.north30.erp.base.material.dto.MaterialCodeMapQueryDTO;
import me.north30.erp.base.material.dto.MaterialQueryDTO;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.entity.BaseMaterialCodeMap;
import me.north30.erp.base.material.mapper.BaseMaterialCodeMapMapper;
import me.north30.erp.base.material.mapper.BaseMaterialMapper;
import me.north30.erp.common.constant.PageConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 物料查询策略：列表/导出查询条件构造、分页参数归一、旧码映射分页查询（IO 类，注入本域 Mapper）。
 * <p>注意：接口文档 6.2.1 的 categoryId 筛选语义为"含下级"，其展开依赖分类域
 * 提供后代 ID 批量查询最小接口，本期跨域契约未覆盖，暂按精确匹配落地（偏差见任务报告）。</p>
 */
@Component
@RequiredArgsConstructor
public class MaterialQueryStrategy {

    private final BaseMaterialMapper materialMapper;
    private final BaseMaterialCodeMapMapper codeMapMapper;

    /**
     * 构建物料列表查询条件：编码/名称模糊，分类/类型/来源/状态/ABC/MRP 精确。
     *
     * @param query 查询参数
     * @return LambdaQueryWrapper<BaseMaterial> 查询条件包装器（排序由调用方追加）
     */
    public LambdaQueryWrapper<BaseMaterial> buildListWrapper(MaterialQueryDTO query) {
        return new LambdaQueryWrapper<BaseMaterial>()
            .like(StringUtils.hasText(query.materialCode()), BaseMaterial::getMaterialCode, query.materialCode())
            .like(StringUtils.hasText(query.materialName()), BaseMaterial::getMaterialName, query.materialName())
            .eq(query.categoryId() != null, BaseMaterial::getCategoryId, query.categoryId())
            .eq(query.materialType() != null, BaseMaterial::getMaterialType, query.materialType())
            .eq(query.sourceType() != null, BaseMaterial::getSourceType, query.sourceType())
            .eq(query.status() != null, BaseMaterial::getStatus, query.status())
            .eq(StringUtils.hasText(query.abcClass()), BaseMaterial::getAbcClass, query.abcClass())
            .eq(query.isMrpActive() != null, BaseMaterial::getIsMrpActive, query.isMrpActive());
    }

    /**
     * 物料分页查询（创建时间倒序）。
     *
     * @param query 查询参数
     * @return 物料分页结果
     */
    public Page<BaseMaterial> pageMaterials(MaterialQueryDTO query) {
        return materialMapper.selectPage(
            new Page<>(resolvePageNum(query.pageNum()), resolvePageSize(query.pageSize())),
            buildListWrapper(query).orderByDesc(BaseMaterial::getCreateTime));
    }

    /**
     * 物料列表查询（导出全量，创建时间倒序）。
     *
     * @param query 查询参数
     * @return 物料列表
     */
    public List<BaseMaterial> listMaterials(MaterialQueryDTO query) {
        return materialMapper.selectList(
            buildListWrapper(query).orderByDesc(BaseMaterial::getCreateTime));
    }

    /**
     * 构建旧码映射查询条件：旧编码/新物料编码模糊，来源部门/合并组/迁移批次精确。
     *
     * @param query 查询参数
     * @return LambdaQueryWrapper<BaseMaterialCodeMap> 查询条件包装器（排序由调用方追加）
     */
    public LambdaQueryWrapper<BaseMaterialCodeMap> buildCodeMapWrapper(MaterialCodeMapQueryDTO query) {
        return new LambdaQueryWrapper<BaseMaterialCodeMap>()
            .like(StringUtils.hasText(query.oldCode()), BaseMaterialCodeMap::getOldCode, query.oldCode())
            .like(StringUtils.hasText(query.newMaterialCode()),
                BaseMaterialCodeMap::getNewMaterialCode, query.newMaterialCode())
            .eq(StringUtils.hasText(query.sourceDept()), BaseMaterialCodeMap::getSourceDept, query.sourceDept())
            .eq(StringUtils.hasText(query.mergeGroup()), BaseMaterialCodeMap::getMergeGroup, query.mergeGroup())
            .eq(StringUtils.hasText(query.migrateBatch()),
                BaseMaterialCodeMap::getMigrateBatch, query.migrateBatch());
    }

    /**
     * 旧码映射分页查询（迁移时间倒序）。
     *
     * @param query 查询参数
     * @return 旧码映射分页结果
     */
    public Page<BaseMaterialCodeMap> pageCodeMaps(MaterialCodeMapQueryDTO query) {
        return codeMapMapper.selectPage(
            new Page<>(resolvePageNum(query.pageNum()), resolvePageSize(query.pageSize())),
            buildCodeMapWrapper(query).orderByDesc(BaseMaterialCodeMap::getMigratedAt));
    }

    /**
     * 分页页码归一：缺省第 1 页（防御二层，HTTP 层已有 @Min(1) 校验）。
     *
     * @param pageNum 入参页码
     * @return 归一后页码
     */
    public int resolvePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return PageConstants.DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    /**
     * 每页条数归一：缺省 20 条、上限按 PageConstants.MAX_PAGE_SIZE 截断（绕过 HTTP 直调兜底）。
     *
     * @param pageSize 入参每页条数
     * @return 归一后每页条数
     */
    public int resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, PageConstants.MAX_PAGE_SIZE);
    }
}
