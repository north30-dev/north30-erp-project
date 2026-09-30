package me.north30.erp.system.codesequence.converter;

import me.north30.erp.system.codesequence.entity.SysCodeSequence;
import me.north30.erp.system.codesequence.vo.CodeSequenceVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 单据编号序列域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CodeSequenceConverter {

    /**
     * 序列实体 → VO。
     * <p>nextCodePreview 为派生字段（prefix + period + 补零流水，依赖 seqLength 缺省值），由 Service 计算后显式装配。</p>
     */
    @Mapping(target = "nextCodePreview", ignore = true)
    @Mapping(target = "updateTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    CodeSequenceVO toVO(SysCodeSequence sequence);
}
