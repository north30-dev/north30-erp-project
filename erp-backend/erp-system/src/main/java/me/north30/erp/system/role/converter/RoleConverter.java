package me.north30.erp.system.role.converter;

import me.north30.erp.system.role.dto.RoleCreateDTO;
import me.north30.erp.system.role.dto.RoleDataScopeItemDTO;
import me.north30.erp.system.role.dto.RoleUpdateDTO;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleDataScope;
import me.north30.erp.system.role.entity.SysRoleMenu;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 角色域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleConverter {

    /**
     * 新增角色 DTO → 实体。
     * <p>roleSort/dataScope/status 缺省值与 isBuiltin 由 Service 显式处理。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode")
    @Mapping(target = "roleName")
    @Mapping(target = "roleSort")
    @Mapping(target = "dataScope")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    SysRole toEntity(RoleCreateDTO dto);

    /**
     * 修改角色 DTO 合并到已加载实体（null 字段跳过，实现部分更新语义）。
     * <p>role_code 不可修改；version 随 DTO 透传到实体，供 updateById 乐观锁校验。</p>
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "roleName")
    @Mapping(target = "roleSort")
    @Mapping(target = "dataScope")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    @Mapping(target = "version")
    void updateEntity(RoleUpdateDTO dto, @MappingTarget SysRole role);

    /**
     * 组装角色-菜单关联实体（全删全插场景逐条构建）。
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleId")
    @Mapping(target = "menuId")
    SysRoleMenu toRoleMenu(Long roleId, Long menuId);

    /**
     * 数据范围明细 DTO → 实体。
     * <p>deptIds/userIds 序列化（逗号串）与 fieldMask 缺省值由 Service 处理。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleId", source = "roleId")
    @Mapping(target = "bizObject", source = "item.bizObject")
    @Mapping(target = "filterDimension", source = "item.filterDimension")
    @Mapping(target = "scopeType", source = "item.scopeType")
    SysRoleDataScope toRoleDataScope(Long roleId, RoleDataScopeItemDTO item);
}
