package me.north30.erp.system.config.converter;

import me.north30.erp.system.config.dto.ConfigCreateDTO;
import me.north30.erp.system.config.entity.SysConfig;
import me.north30.erp.system.config.vo.ConfigVO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 系统参数域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ConfigConverter {

    /**
     * 新增参数 DTO → 实体。
     * <p>isSystem/status 缺省值由 Service 显式处理。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "configKey")
    @Mapping(target = "configName")
    @Mapping(target = "configValue")
    @Mapping(target = "valueType")
    @Mapping(target = "configGroup")
    @Mapping(target = "isSystem")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    SysConfig toEntity(ConfigCreateDTO dto);

    /**
     * 参数实体 → VO。
     */
    ConfigVO toVO(SysConfig config);
}
