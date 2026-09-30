package me.north30.erp.system.menu.converter;

import me.north30.erp.system.menu.dto.MenuCreateDTO;
import me.north30.erp.system.menu.dto.MenuUpdateDTO;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.vo.MenuNodeVO;
import me.north30.erp.system.menu.vo.MenuTreeVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 菜单域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MenuConverter {

    /**
     * 新增菜单 DTO → 实体。
     * <p>menuSort/visible/status 缺省值由 Service 显式处理。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "menuName")
    @Mapping(target = "parentId")
    @Mapping(target = "menuType")
    @Mapping(target = "path")
    @Mapping(target = "component")
    @Mapping(target = "perms")
    @Mapping(target = "icon")
    @Mapping(target = "menuSort")
    @Mapping(target = "visible")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    SysMenu toEntity(MenuCreateDTO dto);

    /**
     * 修改菜单 DTO 合并到已加载实体（null 字段跳过，实现部分更新语义）。
     * <p>parentId/menuType 合法性校验、按钮字段清空与乐观锁 version 由 Service 处理。</p>
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "menuName")
    @Mapping(target = "parentId")
    @Mapping(target = "menuType")
    @Mapping(target = "path")
    @Mapping(target = "component")
    @Mapping(target = "perms")
    @Mapping(target = "icon")
    @Mapping(target = "menuSort")
    @Mapping(target = "visible")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    void updateEntity(MenuUpdateDTO dto, @MappingTarget SysMenu menu);

    /**
     * 实体 → 树节点 VO（children 由树构建逻辑装配）。
     */
    @Mapping(target = "children", ignore = true)
    MenuNodeVO toNodeVO(SysMenu menu);

    /**
     * 实体 → 菜单树 VO（children 由树构建逻辑装配）。
     */
    @Mapping(target = "menuId", source = "id")
    @Mapping(target = "children", ignore = true)
    MenuTreeVO toTreeVO(SysMenu menu);
}
