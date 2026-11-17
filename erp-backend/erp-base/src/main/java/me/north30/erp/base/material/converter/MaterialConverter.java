package me.north30.erp.base.material.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import me.north30.erp.base.material.dto.MaterialCodeMapCreateDTO;
import me.north30.erp.base.material.dto.MaterialCreateDTO;
import me.north30.erp.base.material.dto.MaterialUpdateDTO;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.entity.BaseMaterialCodeMap;
import me.north30.erp.base.material.vo.MaterialCodeMapVO;
import me.north30.erp.base.material.vo.MaterialDetailVO;
import me.north30.erp.base.material.vo.MaterialVO;
import me.north30.erp.common.spi.DictOptionsResolver;
import me.north30.erp.common.util.CsvUtil;
import me.north30.erp.common.util.DateTimeFormatUtil;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 物料域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。</p>
 * <p>ignoreByDefault + 显式声明映射字段：目标字段未声明映射即不拷贝，杜绝静默丢字段。
 * 金额/数量转换经 {@link #parseDecimal}/{@link #formatDecimal} 统一走 BigDecimal
 * （数据库 DECIMAL(20,6)，接口层 6 位小数字符串口径）。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MaterialConverter {

    /**
     * 新增物料 DTO → 实体。
     * <p>物料编码（服务端生成）、创建默认值（applyCreateDefault）等由 Service 编排处理，不在此映射。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "materialName")
    @Mapping(target = "spec")
    @Mapping(target = "brand")
    @Mapping(target = "categoryId")
    @Mapping(target = "materialType")
    @Mapping(target = "sourceType")
    @Mapping(target = "unit")
    @Mapping(target = "drawingNo")
    @Mapping(target = "drawingVersion")
    @Mapping(target = "standardCost", source = "standardCost", qualifiedByName = "parseDecimal")
    @Mapping(target = "safetyStock", source = "safetyStock", qualifiedByName = "parseDecimal")
    @Mapping(target = "maxStock", source = "maxStock", qualifiedByName = "parseDecimal")
    @Mapping(target = "minOrderQty", source = "minOrderQty", qualifiedByName = "parseDecimal")
    @Mapping(target = "abcClass")
    @Mapping(target = "leadTime")
    @Mapping(target = "productionLeadTime")
    @Mapping(target = "costMethod")
    @Mapping(target = "inventorySubjectId")
    @Mapping(target = "costSubjectId")
    @Mapping(target = "revenueSubjectId")
    @Mapping(target = "defaultSupplierId")
    @Mapping(target = "defaultWarehouseId")
    @Mapping(target = "defaultLocationId")
    @Mapping(target = "isBatchManaged")
    @Mapping(target = "isSerialManaged")
    @Mapping(target = "isShelfLife")
    @Mapping(target = "shelfLifeDays")
    @Mapping(target = "isMrpActive")
    @Mapping(target = "remark")
    BaseMaterial create(MaterialCreateDTO dto);

    /**
     * 修改物料 DTO → 部分更新实体：null 字段不覆盖库内值（选填字段 null 表示不修改）。
     * <p>物料编码不可修改（无映射字段即天然不拷贝）；携带乐观锁 version。</p>
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "materialName")
    @Mapping(target = "spec")
    @Mapping(target = "brand")
    @Mapping(target = "categoryId")
    @Mapping(target = "materialType")
    @Mapping(target = "sourceType")
    @Mapping(target = "unit")
    @Mapping(target = "drawingNo")
    @Mapping(target = "drawingVersion")
    @Mapping(target = "standardCost", source = "standardCost", qualifiedByName = "parseDecimal")
    @Mapping(target = "safetyStock", source = "safetyStock", qualifiedByName = "parseDecimal")
    @Mapping(target = "maxStock", source = "maxStock", qualifiedByName = "parseDecimal")
    @Mapping(target = "minOrderQty", source = "minOrderQty", qualifiedByName = "parseDecimal")
    @Mapping(target = "abcClass")
    @Mapping(target = "leadTime")
    @Mapping(target = "productionLeadTime")
    @Mapping(target = "costMethod")
    @Mapping(target = "inventorySubjectId")
    @Mapping(target = "costSubjectId")
    @Mapping(target = "revenueSubjectId")
    @Mapping(target = "defaultSupplierId")
    @Mapping(target = "defaultWarehouseId")
    @Mapping(target = "defaultLocationId")
    @Mapping(target = "isBatchManaged")
    @Mapping(target = "isSerialManaged")
    @Mapping(target = "isShelfLife")
    @Mapping(target = "shelfLifeDays")
    @Mapping(target = "isMrpActive")
    @Mapping(target = "remark")
    @Mapping(target = "version")
    BaseMaterial update(MaterialUpdateDTO dto);

    /**
     * 旧码映射新增 DTO → 实体（newMaterialId/newMaterialCode 由 Service 经 bindNewMaterial 回填）。
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "oldCode")
    @Mapping(target = "oldName")
    @Mapping(target = "sourceDept")
    @Mapping(target = "mergeGroup")
    @Mapping(target = "migrateBatch")
    BaseMaterialCodeMap createCodeMap(MaterialCodeMapCreateDTO dto);

    /**
     * 实体 → 列表项 VO（categoryName 由 Service 批量查询后传入，避免 N+1）。
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "materialCode")
    @Mapping(target = "materialName")
    @Mapping(target = "spec")
    @Mapping(target = "brand")
    @Mapping(target = "categoryId")
    @Mapping(target = "categoryName", source = "categoryName")
    @Mapping(target = "materialType")
    @Mapping(target = "sourceType")
    @Mapping(target = "unit")
    @Mapping(target = "status")
    @Mapping(target = "abcClass")
    @Mapping(target = "standardCost", source = "entity.standardCost", qualifiedByName = "formatDecimal")
    @Mapping(target = "safetyStock", source = "entity.safetyStock", qualifiedByName = "formatDecimal")
    @Mapping(target = "maxStock", source = "entity.maxStock", qualifiedByName = "formatDecimal")
    @Mapping(target = "minOrderQty", source = "entity.minOrderQty", qualifiedByName = "formatDecimal")
    @Mapping(target = "leadTime")
    @Mapping(target = "productionLeadTime")
    @Mapping(target = "costMethod")
    @Mapping(target = "isBatchManaged")
    @Mapping(target = "isSerialManaged")
    @Mapping(target = "isShelfLife")
    @Mapping(target = "isMrpActive")
    MaterialVO toVO(BaseMaterial entity, String categoryName);

    /**
     * 实体列表 → VO 列表（categoryName 经预载名称映射回填）。
     *
     * @param entities      物料实体列表
     * @param categoryNames 分类 ID → 分类名称映射
     * @return 列表项 VO 列表
     */
    default List<MaterialVO> toVOs(List<BaseMaterial> entities, Map<Long, String> categoryNames) {
        return entities.stream()
            .map(entity -> toVO(entity, categoryNames.get(entity.getCategoryId())))
            .toList();
    }

    /**
     * 实体 → 详情 VO（categoryName 由 Service 批量查询后传入）。
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "materialCode")
    @Mapping(target = "materialName")
    @Mapping(target = "spec")
    @Mapping(target = "brand")
    @Mapping(target = "categoryId")
    @Mapping(target = "categoryName", source = "categoryName")
    @Mapping(target = "materialType")
    @Mapping(target = "sourceType")
    @Mapping(target = "unit")
    @Mapping(target = "drawingNo")
    @Mapping(target = "drawingVersion")
    @Mapping(target = "standardCost", source = "entity.standardCost", qualifiedByName = "formatDecimal")
    @Mapping(target = "safetyStock", source = "entity.safetyStock", qualifiedByName = "formatDecimal")
    @Mapping(target = "maxStock", source = "entity.maxStock", qualifiedByName = "formatDecimal")
    @Mapping(target = "minOrderQty", source = "entity.minOrderQty", qualifiedByName = "formatDecimal")
    @Mapping(target = "abcClass")
    @Mapping(target = "leadTime")
    @Mapping(target = "productionLeadTime")
    @Mapping(target = "costMethod")
    @Mapping(target = "inventorySubjectId")
    @Mapping(target = "costSubjectId")
    @Mapping(target = "revenueSubjectId")
    @Mapping(target = "defaultSupplierId")
    @Mapping(target = "defaultWarehouseId")
    @Mapping(target = "defaultLocationId")
    @Mapping(target = "isBatchManaged")
    @Mapping(target = "isSerialManaged")
    @Mapping(target = "isShelfLife")
    @Mapping(target = "shelfLifeDays")
    @Mapping(target = "isMrpActive")
    @Mapping(target = "status")
    @Mapping(target = "version")
    @Mapping(target = "createBy")
    @Mapping(target = "createTime", source = "entity.createTime", qualifiedByName = "formatTime")
    @Mapping(target = "updateBy")
    @Mapping(target = "updateTime", source = "entity.updateTime", qualifiedByName = "formatTime")
    @Mapping(target = "remark")
    MaterialDetailVO toDetailVO(BaseMaterial entity, String categoryName);

    /**
     * 旧码映射实体 → VO。
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "oldCode")
    @Mapping(target = "oldName")
    @Mapping(target = "sourceDept")
    @Mapping(target = "mergeGroup")
    @Mapping(target = "newMaterialId")
    @Mapping(target = "newMaterialCode")
    @Mapping(target = "migrateBatch")
    @Mapping(target = "migratedAt")
    MaterialCodeMapVO toCodeMapVO(BaseMaterialCodeMap entity);

    /**
     * 旧码映射实体列表 → VO 列表。
     *
     * @param entities 旧码映射实体列表
     * @return VO 列表
     */
    default List<MaterialCodeMapVO> toCodeMapVOs(List<BaseMaterialCodeMap> entities) {
        return entities.stream().map(this::toCodeMapVO).toList();
    }

    /**
     * 导出物料列表为 CSV 字节（委托公共 {@link CsvUtil}：UTF-8 BOM + CRLF + 自动转义，
     * 列与列表页一致）。
     * <p>物料类型/来源类型/状态的展示文案经 {@link DictOptionsResolver} 预载的字典 label 渲染
     * （消除文案硬编码，字典运营改词后导出自动跟随），字典缺失该项时回退默认文案兜底。</p>
     *
     * @param entities      物料实体列表
     * @param categoryNames 分类 ID → 分类名称映射
     * @param typeLabels    物料类型字典 value→label（material_type）
     * @param sourceLabels  来源类型字典 value→label（source_type）
     * @param statusLabels  物料状态字典 value→label（material_status）
     * @return CSV 文件内容字节
     */
    default byte[] toCsvBytes(List<BaseMaterial> entities, Map<Long, String> categoryNames,
                              Map<String, String> typeLabels, Map<String, String> sourceLabels,
                              Map<String, String> statusLabels) {
        List<String> header = List.of("物料ID", "物料编码", "物料名称", "规格型号", "品牌", "物料分类", "物料类型",
            "来源类型", "计量单位", "状态", "ABC分类", "标准成本", "安全库存", "最大库存", "最小采购批量",
            "采购提前期", "生产提前期", "是否批次管理", "是否序列号管理", "是否保质期管理", "参与MRP", "创建时间");
        List<List<String>> rows = entities.stream()
            .map(e -> List.of(
                String.valueOf(e.getId()),
                nvl(e.getMaterialCode()), nvl(e.getMaterialName()), nvl(e.getSpec()), nvl(e.getBrand()),
                nvl(categoryNames.get(e.getCategoryId())),
                labelOrFallback(typeLabels, e.getMaterialType(), defaultTypeText(e.getMaterialType())),
                labelOrFallback(sourceLabels, e.getSourceType(), defaultSourceText(e.getSourceType())),
                nvl(e.getUnit()), labelOrFallback(statusLabels, e.getStatus(), defaultStatusText(e.getStatus())),
                nvl(e.getAbcClass()),
                nvl(formatDecimal(e.getStandardCost())), nvl(formatDecimal(e.getSafetyStock())),
                nvl(formatDecimal(e.getMaxStock())), nvl(formatDecimal(e.getMinOrderQty())),
                nvl(String.valueOf(e.getLeadTime())), nvl(String.valueOf(e.getProductionLeadTime())),
                Boolean.TRUE.equals(e.getIsBatchManaged()) ? "是" : "否",
                Boolean.TRUE.equals(e.getIsSerialManaged()) ? "是" : "否",
                Boolean.TRUE.equals(e.getIsShelfLife()) ? "是" : "否",
                Integer.valueOf(1).equals(e.getIsMrpActive()) ? "是" : "否",
                nvl(DateTimeFormatUtil.format(e.getCreateTime()))))
            .toList();
        return CsvUtil.toCsvBytes(header, rows);
    }

    /**
     * 字典 label 渲染：命中字典返回 label，缺失回退默认文案。
     */
    private static String labelOrFallback(Map<String, String> labels, Integer value, String fallback) {
        if (value == null) {
            return "";
        }
        return labels.getOrDefault(String.valueOf(value), fallback);
    }

    /**
     * null 安全文本（CSV 单元格 null 显示为空串）。
     */
    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    /**
     * 金额/数量 BigDecimal → 6 位小数字符串（null 安全，接口文档 1.7 口径）。
     */
    @Named("formatDecimal")
    default String formatDecimal(BigDecimal value) {
        return value == null ? null : value.setScale(6, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * 金额/数量字符串 → BigDecimal（HTTP 层 @Pattern 已保证非负数字格式，null/空串直通）。
     */
    @Named("parseDecimal")
    default BigDecimal parseDecimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    /**
     * 时间 → "yyyy-MM-dd HH:mm:ss" 字符串（委托 DateTimeFormatUtil，null 安全）。
     */
    @Named("formatTime")
    default String formatTime(java.time.LocalDateTime time) {
        return DateTimeFormatUtil.format(time);
    }

    /**
     * 物料类型默认文案（字典缺失时的兜底，与 01_seed.sql 词表一致）。
     */
    private static String defaultTypeText(Integer type) {
        return switch (type == null ? 0 : type) {
            case 1 -> "原材料";
            case 2 -> "半成品";
            case 3 -> "成品";
            case 4 -> "辅料";
            default -> "";
        };
    }

    /**
     * 来源类型默认文案（字典缺失时的兜底，与 01_seed.sql 词表一致）。
     */
    private static String defaultSourceText(Integer type) {
        return switch (type == null ? 0 : type) {
            case 1 -> "自制";
            case 2 -> "外购";
            case 3 -> "外协";
            default -> "";
        };
    }

    /**
     * 物料状态默认文案（字典缺失时的兜底，与 01_seed.sql 词表一致）。
     */
    private static String defaultStatusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "新建";
            case 1 -> "审核";
            case 2 -> "启用";
            case 3 -> "停用";
            default -> "";
        };
    }
}
