package me.north30.erp.base.material.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.common.MaterialDictKeys;
import me.north30.erp.base.material.converter.MaterialConverter;
import me.north30.erp.base.material.dto.MaterialAuditDTO;
import me.north30.erp.base.material.dto.MaterialBatchDeleteDTO;
import me.north30.erp.base.material.dto.MaterialCodeMapCreateDTO;
import me.north30.erp.base.material.dto.MaterialCodeMapQueryDTO;
import me.north30.erp.base.material.dto.MaterialCreateDTO;
import me.north30.erp.base.material.dto.MaterialDisableDTO;
import me.north30.erp.base.material.dto.MaterialQueryDTO;
import me.north30.erp.base.material.dto.MaterialUpdateDTO;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.entity.BaseMaterialCodeMap;
import me.north30.erp.base.material.service.MaterialApplicationService;
import me.north30.erp.base.material.service.MaterialDomainService;
import me.north30.erp.base.material.strategy.MaterialCodeGeneratorStrategy;
import me.north30.erp.base.material.strategy.MaterialImportStrategy;
import me.north30.erp.base.material.vo.MaterialBatchDeleteVO;
import me.north30.erp.base.material.vo.MaterialCodeMapCreateVO;
import me.north30.erp.base.material.vo.MaterialCodeMapVO;
import me.north30.erp.base.material.vo.MaterialCreateVO;
import me.north30.erp.base.material.vo.MaterialDeleteVO;
import me.north30.erp.base.material.vo.MaterialDetailVO;
import me.north30.erp.base.material.vo.MaterialImportResultVO;
import me.north30.erp.base.material.vo.MaterialStatusVO;
import me.north30.erp.base.material.vo.MaterialVO;
import me.north30.erp.base.materialcategory.service.MaterialCategoryDomainService;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.spi.DictOptionsResolver;
import me.north30.erp.common.spi.DictValidator;
import me.north30.erp.common.util.DateTimeFormatUtil;

/**
 * 物料应用服务实现（纯编排，零私有方法，对齐 SysUserApplicationServiceImpl 模板）。
 * <p>职责分工：状态机与删除守卫进实体/状态机策略，纯规则与 Mapper 网关进
 * {@link MaterialDomainService}，DTO/VO/CSV 转换进 {@link MaterialConverter}，
 * CSV 解析与导入单行装配进 {@link MaterialImportStrategy}；本类只负责
 * "跨域校验编排 → 转换 → 持久化 → 组装响应"。</p>
 * <p>跨域依赖仅 {@link MaterialCategoryDomainService} 最小接口（启用分类编码/叶子标记/分类名称批量）
 * 与 {@link DictValidator} SPI（计量单位字典校验）；不注入任何 Mapper。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialApplicationServiceImpl implements MaterialApplicationService {

    /** 计量单位字典类型（sys_dict_type.dict_type，key 收口 {@link MaterialDictKeys}） */
    private static final String DICT_TYPE_UNIT = MaterialDictKeys.UNIT;

    /** 导入模式：整批事务（任一行错误整批回滚） */
    private static final String MODE_ALL_OR_NOTHING = "ALL_OR_NOTHING";

    /** 导入模式：跳过错误行，成功行照常入库 */
    private static final String MODE_SKIP_ERROR = "SKIP_ERROR";

    private final MaterialDomainService materialDomainService;
    private final MaterialCategoryDomainService materialCategoryDomainService;
    private final MaterialConverter materialConverter;
    private final MaterialCodeGeneratorStrategy materialCodeGeneratorStrategy;
    private final MaterialImportStrategy materialImportStrategy;
    private final DictValidator dictValidator;
    private final DictOptionsResolver dictOptionsResolver;

    /**
     * 物料分页查询（分类名称一次 IN 批量回填，禁 N+1）。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<MaterialVO> page(MaterialQueryDTO query) {
        Page<BaseMaterial> page = materialDomainService.pageMaterials(query);
        Map<Long, String> categoryNames = materialCategoryDomainService.getCategoryNameMap(
            page.getRecords().stream().map(BaseMaterial::getCategoryId).collect(Collectors.toSet()));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
            materialConverter.toVOs(page.getRecords(), categoryNames));
    }

    /**
     * 物料详情。
     */
    @Override
    @Transactional(readOnly = true)
    public MaterialDetailVO getDetail(Long id) {
        BaseMaterial material = materialDomainService.requireMaterial(id);
        return materialConverter.toDetailVO(material, materialCategoryDomainService
            .getCategoryNameMap(Set.of(material.getCategoryId())).get(material.getCategoryId()));
    }

    /**
     * 按编码查询物料（扫码/编码录入即时回显）。
     */
    @Override
    @Transactional(readOnly = true)
    public MaterialDetailVO getByCode(String materialCode) {
        BaseMaterial material = materialDomainService.requireMaterialByCode(materialCode);
        return materialConverter.toDetailVO(material, materialCategoryDomainService
            .getCategoryNameMap(Set.of(material.getCategoryId())).get(material.getCategoryId()));
    }

    /**
     * 新增物料：分类须启用且为叶子（11017/11005）、计量单位须在字典 unit 内（11016）、
     * 四要素重复提示（11008）、编码服务端生成（分类编码前 4 位 + 6 位流水，用尽 11014）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialCreateVO create(MaterialCreateDTO dto) {
        String categoryCode = materialCategoryDomainService.requireEnabledCategoryCode(dto.categoryId());
        if (!materialCategoryDomainService.isLeaf(dto.categoryId())) {
            throw new BusinessException(BaseErrorCode.MATERIAL_CATEGORY_NOT_LEAF, "物料分类须选择叶子节点");
        }
        dictValidator.requireEnabledItem(DICT_TYPE_UNIT, dto.unit());
        materialDomainService.requireFourElementsAvailable(dto.materialName(), dto.spec(), dto.brand(),
            dto.unit(), null);
        BaseMaterial material = materialConverter.create(dto);
        material.requireShelfLifeConsistency();
        material.applyCreateDefault();
        material.setMaterialCode(materialCodeGeneratorStrategy.generate(categoryCode));
        materialDomainService.insert(material);
        log.info("物料新增完成：id={}, materialCode={}, categoryId={}",
            material.getId(), material.getMaterialCode(), material.getCategoryId());
        return new MaterialCreateVO(material.getId(), material.getMaterialCode(), material.getStatus());
    }

    /**
     * 修改物料（部分更新 + 乐观锁，编码不可修改）：变更分类须启用叶子分类、
     * 计量单位字典校验、保质期开关一致性守卫、四要素重复提示、计价方法不可变更（11012）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String update(Long id, MaterialUpdateDTO dto) {
        BaseMaterial current = materialDomainService.requireMaterial(id);
        if (dto.costMethod() != null && current.getCostMethod() != null
            && dto.costMethod().intValue() != current.getCostMethod().intValue()) {
            throw new BusinessException(BaseErrorCode.MATERIAL_COST_METHOD_IMMUTABLE);
        }
        if (dto.categoryId() != null) {
            materialCategoryDomainService.requireEnabledCategoryCode(dto.categoryId());
            if (!materialCategoryDomainService.isLeaf(dto.categoryId())) {
                throw new BusinessException(BaseErrorCode.MATERIAL_CATEGORY_NOT_LEAF, "物料分类须选择叶子节点");
            }
        }
        if (dto.unit() != null) {
            dictValidator.requireEnabledItem(DICT_TYPE_UNIT, dto.unit());
        }
        BaseMaterial patch = materialConverter.update(dto);
        patch.requireShelfLifeConsistency();
        materialDomainService.requireFourElementsAvailable(
            patch.getMaterialName() != null ? patch.getMaterialName() : current.getMaterialName(),
            patch.getSpec() != null ? patch.getSpec() : current.getSpec(),
            patch.getBrand() != null ? patch.getBrand() : current.getBrand(),
            patch.getUnit() != null ? patch.getUnit() : current.getUnit(), id);
        patch.setId(id);
        patch.setUpdateTime(LocalDateTime.now());
        materialDomainService.updateRow(patch);
        return DateTimeFormatUtil.format(patch.getUpdateTime());
    }

    /**
     * 删除物料（仅 0-新建可删除；BOM/单据引用计数在对应域落地前恒为 0）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialDeleteVO delete(Long id) {
        BaseMaterial material = materialDomainService.requireMaterial(id);
        materialDomainService.requireDeletable(material, 0L);
        materialDomainService.deleteById(id);
        return new MaterialDeleteVO(id, 1);
    }

    /**
     * 批量删除物料：一次批量加载（禁循环内查库），逐行守卫，部分失败逐行采集原因
     * （接口文档 13.1 部分失败语义，单行失败不中断批次）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialBatchDeleteVO batchDelete(MaterialBatchDeleteDTO dto) {
        Map<Long, BaseMaterial> materials = materialDomainService.listByIds(dto.ids()).stream()
            .collect(Collectors.toMap(BaseMaterial::getId, m -> m, (a, b) -> a));
        List<MaterialBatchDeleteVO.Failure> failures = new ArrayList<>();
        int successCount = 0;
        for (Long id : dto.ids()) {
            BaseMaterial material = materials.get(id);
            if (material == null) {
                failures.add(new MaterialBatchDeleteVO.Failure(id, null,
                    BaseErrorCode.MATERIAL_NOT_FOUND.getCode(), "物料 " + id + " 不存在"));
                continue;
            }
            try {
                materialDomainService.requireDeletable(material, 0L);
                materialDomainService.deleteById(id);
                successCount++;
            } catch (BusinessException e) {
                // 部分失败语义：单行失败仅采集原因，不中断批次、不吞异常
                failures.add(new MaterialBatchDeleteVO.Failure(id, material.getMaterialCode(),
                    e.getCode(), e.getMessage()));
            }
        }
        log.info("物料批量删除完成：请求 {} 条，成功 {} 条，失败 {} 条",
            dto.ids().size(), successCount, failures.size());
        return new MaterialBatchDeleteVO(successCount, failures.size(), failures);
    }

    /**
     * 物料审核（0-新建 → 1-审核/驳回，非法前置 11010）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialStatusVO audit(Long id, MaterialAuditDTO dto) {
        return new MaterialStatusVO(materialDomainService.audit(id, Boolean.TRUE.equals(dto.approved())), null);
    }

    /**
     * 物料启用（1-审核 → 2-启用，非法前置 11010/11003）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialStatusVO enable(Long id) {
        return new MaterialStatusVO(materialDomainService.enable(id), null);
    }

    /**
     * 物料停用（1-审核/2-启用 → 3-停用；affectedBomCount 在 BOM 域落地前恒为 0）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialStatusVO disable(Long id, MaterialDisableDTO dto) {
        return new MaterialStatusVO(materialDomainService.disable(id, dto.reason()), 0);
    }

    /**
     * 物料批量导入：ALL_OR_NOTHING 任一行错误整批回滚（抛汇总异常）、SKIP_ERROR 跳过错误行；
     * 四要素查重经预载索引内存完成；单行装配落库收敛于导入策略。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialImportResultVO importMaterials(MultipartFile file, String mode) {
        String importMode = MODE_SKIP_ERROR.equals(mode) ? MODE_SKIP_ERROR : MODE_ALL_OR_NOTHING;
        List<MaterialCreateDTO> rows = materialImportStrategy.parse(file);
        String importBatch = materialImportStrategy.nextImportBatch();
        Map<String, String> fourElementIndex = materialDomainService.loadFourElementIndex();
        List<MaterialImportResultVO.RowError> errors = new ArrayList<>();
        int successRows = 0;
        for (int i = 0; i < rows.size(); i++) {
            try {
                materialImportStrategy.buildAndInsert(rows.get(i), fourElementIndex);
                successRows++;
            } catch (BusinessException e) {
                // 部分失败采集：ALL_OR_NOTHING 模式最后整批抛出回滚，SKIP_ERROR 模式跳过该行
                errors.add(new MaterialImportResultVO.RowError(i + 1, e.getCode(), e.getMessage()));
            }
        }
        log.info("物料导入完成：批次={}, 总行数={}, 成功={}, 失败={}",
            importBatch, rows.size(), successRows, errors.size());
        if (MODE_ALL_OR_NOTHING.equals(importMode) && !errors.isEmpty()) {
            MaterialImportResultVO.RowError first = errors.get(0);
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "导入整批回滚：共 " + errors.size() + " 行错误，首行错误（第 " + first.rowNo() + " 行）"
                    + first.errorMessage());
        }
        return new MaterialImportResultVO(importBatch, rows.size(), successRows, errors.size(), errors);
    }

    /**
     * 物料列表导出 CSV（查询条件复用分页接口，分类名称批量回填；物料类型/来源类型/状态
     * 展示文案经 {@link DictOptionsResolver} 预载字典 label 渲染，一次查询禁行级查库）。
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(MaterialQueryDTO query) {
        List<BaseMaterial> materials = materialDomainService.listMaterials(query);
        Map<Long, String> categoryNames = materialCategoryDomainService.getCategoryNameMap(
            materials.stream().map(BaseMaterial::getCategoryId).collect(Collectors.toSet()));
        return materialConverter.toCsvBytes(materials, categoryNames,
            dictOptionsResolver.loadItemLabels(MaterialDictKeys.MATERIAL_TYPE),
            dictOptionsResolver.loadItemLabels(MaterialDictKeys.SOURCE_TYPE),
            dictOptionsResolver.loadItemLabels(MaterialDictKeys.MATERIAL_STATUS));
    }

    /**
     * 物料导入模板下载（CSV，首行表头）。
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] downloadImportTemplate() {
        return materialImportStrategy.template();
    }

    /**
     * 旧码映射分页查询。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<MaterialCodeMapVO> pageCodeMaps(MaterialCodeMapQueryDTO query) {
        Page<BaseMaterialCodeMap> page = materialDomainService.pageCodeMaps(query);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
            materialConverter.toCodeMapVOs(page.getRecords()));
    }

    /**
     * 新增旧码映射：新物料必须存在（11002）、old_code + source_dept 唯一（11015），
     * 新物料编码由服务端按 newMaterialId 回填。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialCodeMapCreateVO createCodeMap(MaterialCodeMapCreateDTO dto) {
        BaseMaterial newMaterial = materialDomainService.requireNewMaterialExists(dto.newMaterialId());
        materialDomainService.requireMappingAvailable(dto.oldCode(), dto.sourceDept(), null);
        BaseMaterialCodeMap codeMap = materialConverter.createCodeMap(dto);
        codeMap.bindNewMaterial(newMaterial.getId(), newMaterial.getMaterialCode());
        codeMap.applyCreateDefault();
        materialDomainService.insertCodeMap(codeMap);
        log.info("旧码映射新增完成：id={}, oldCode={}, newMaterialCode={}",
            codeMap.getId(), codeMap.getOldCode(), codeMap.getNewMaterialCode());
        return new MaterialCodeMapCreateVO(codeMap.getId(), codeMap.getNewMaterialCode());
    }
}
