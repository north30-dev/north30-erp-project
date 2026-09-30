package me.north30.erp.system.dict.converter;

import me.north30.erp.system.dict.dto.DictItemCreateDTO;
import me.north30.erp.system.dict.dto.DictItemUpdateDTO;
import me.north30.erp.system.dict.dto.DictTypeCreateDTO;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.entity.SysDictType;
import me.north30.erp.system.dict.vo.DictItemVO;
import me.north30.erp.system.dict.vo.DictTypeVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 字典域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DictConverter {

    /**
     * 新增字典项 DTO → 实体。
     * <p>lang 缺省值（zh-CN）由 Service 显式处理。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "dictType")
    @Mapping(target = "itemLabel")
    @Mapping(target = "itemValue")
    @Mapping(target = "itemSort")
    @Mapping(target = "cssClass")
    @Mapping(target = "isDefault")
    @Mapping(target = "extJson")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    SysDictItem toEntity(DictItemCreateDTO dto);

    /**
     * 修改字典项 DTO 合并到已加载实体（null 字段跳过，实现部分更新语义）。
     * <p>乐观锁 version 由 Service 处理。</p>
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "itemLabel")
    @Mapping(target = "itemSort")
    @Mapping(target = "cssClass")
    @Mapping(target = "isDefault")
    @Mapping(target = "extJson")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    void updateEntity(DictItemUpdateDTO dto, @MappingTarget SysDictItem item);

    /**
     * 字典项实体 → VO（cached 恒为 false，字典缓存后续阶段接入）。
     */
    @Mapping(target = "cached", constant = "false")
    DictItemVO toVO(SysDictItem item);

    /**
     * 新增字典类型 DTO → 实体。
     * <p>status 缺省值由 Service 显式处理。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "dictType")
    @Mapping(target = "dictName")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    SysDictType toEntity(DictTypeCreateDTO dto);

    /**
     * 字典类型实体 → VO（createTime 统一格式化，itemCount 为派生统计值）。
     */
    @Mapping(target = "createTime", source = "type.createTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "itemCount", source = "itemCount")
    DictTypeVO toVO(SysDictType type, long itemCount);
}
