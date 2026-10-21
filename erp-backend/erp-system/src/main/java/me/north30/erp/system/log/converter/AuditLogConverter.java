package me.north30.erp.system.log.converter;

import me.north30.erp.system.log.entity.SysAuditLog;
import me.north30.erp.system.log.vo.AuditLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 审计日志域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditLogConverter {

    /**
     * 审计日志实体 → VO（diffFields 为详情接口派生字段，由服务层 withDiffFields 填充）。
     */
    @Mapping(target = "diffFields", ignore = true)
    @Mapping(target = "withDiffFields", ignore = true)
    AuditLogVO toVO(SysAuditLog entity);
}
