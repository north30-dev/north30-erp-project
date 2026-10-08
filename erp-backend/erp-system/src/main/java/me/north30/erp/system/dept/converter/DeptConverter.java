package me.north30.erp.system.dept.converter;

import me.north30.erp.system.dept.dto.DeptCreateDTO;
import me.north30.erp.system.dept.dto.DeptUpdateDTO;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.vo.DeptTreeVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 组织域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>
 * 与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。
 * </p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DeptConverter {

    /**
     * 新增组织 DTO → 实体。
     * <p>
     * deptLevel/ancestors 派生字段与 deptSort/status 缺省值由 Service 显式处理。
     * </p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "deptCode")
    @Mapping(target = "deptName")
    @Mapping(target = "parentId")
    @Mapping(target = "deptType")
    @Mapping(target = "leader")
    @Mapping(target = "phone")
    @Mapping(target = "deptSort")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    SysDept toCreatedEntity(DeptCreateDTO dto);

    /**
     * 更新组织 DTO → 实体。
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "deptName")
    @Mapping(target = "parentId")
    @Mapping(target = "deptType")
    @Mapping(target = "leader", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "phone", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "deptSort", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "status", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "remark", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "version")
    void toUpdatedEntity(DeptUpdateDTO dto, @MappingTarget SysDept dept);

    /**
     * 实体 → 组织树 VO（children 由树构建逻辑装配）。
     */
    @Mapping(target = "children", ignore = true)
    DeptTreeVO toTreeVO(SysDept dept);
}
