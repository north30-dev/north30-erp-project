package me.north30.erp.base.materialcategory.converter;

import me.north30.erp.base.materialcategory.dto.CategoryCreateDTO;
import me.north30.erp.base.materialcategory.dto.CategoryUpdateDTO;
import me.north30.erp.base.materialcategory.entity.BaseMaterialCategory;
import me.north30.erp.base.materialcategory.vo.CategoryMutationVO;
import me.north30.erp.base.materialcategory.vo.CategoryStatusVO;
import me.north30.erp.base.materialcategory.vo.CategoryTreeVO;
import me.north30.erp.common.util.DateTimeFormatUtil;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 物料分类域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。</p>
 * <p>ignoreByDefault + 显式声明映射字段：目标字段未声明映射即不拷贝，杜绝静默丢字段。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryConverter {

    /**
     * 新增分类 DTO → 实体。
     * <p>仅映射透传字段；层级推导（applyParent）、编码联动校验（applyCategoryCode）、
     * 状态值域守卫（changeStatus）、显示顺序与创建默认值由 Service 编排显式处理，不在此映射。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "categoryName")
    @Mapping(target = "remark")
    BaseMaterialCategory create(CategoryCreateDTO dto);

    /**
     * 修改分类 DTO → 部分更新实体：null 字段不覆盖库内值（选填字段 null 表示不修改）。
     * <p>编码与父级不可修改（无映射字段即天然不拷贝）；携带乐观锁 version。</p>
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "categoryName")
    @Mapping(target = "categorySort")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    @Mapping(target = "version")
    BaseMaterialCategory update(CategoryUpdateDTO dto);

    /**
     * 实体 → 树 VO（materialCount 派生字段本阶段不装配、children 由树构造策略装配）。
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "categoryCode")
    @Mapping(target = "categoryName")
    @Mapping(target = "parentId")
    @Mapping(target = "categoryLevel")
    @Mapping(target = "isLeaf")
    @Mapping(target = "categorySort")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    @Mapping(target = "version")
    CategoryTreeVO toTreeVO(BaseMaterialCategory entity);

    /**
     * 实体 → 变更响应 VO（createBy/updateBy 本接口不返回）。
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id")
    @Mapping(target = "categoryLevel")
    @Mapping(target = "isLeaf")
    @Mapping(target = "updateTime", source = "entity.updateTime", qualifiedByName = "formatTime")
    @Mapping(target = "isDeleted")
    CategoryMutationVO toMutationVO(BaseMaterialCategory entity);

    /**
     * 状态变更响应 VO。
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "status")
    CategoryStatusVO toStatusVO(BaseMaterialCategory entity);

    /**
     * 时间 → "yyyy-MM-dd HH:mm:ss" 字符串（委托 DateTimeFormatUtil，null 安全）。
     */
    @Named("formatTime")
    default String formatTime(java.time.LocalDateTime time) {
        return DateTimeFormatUtil.format(time);
    }
}
