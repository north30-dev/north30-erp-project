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
     * 序列实体 → VO（含下一编号预览）。
     * <p>nextCodePreview 为派生字段（prefix + period + 补零流水，依赖 seqLength 缺省值 3），
     * 在此统一计算，服务层不再手工装配。</p>
     */
    default CodeSequenceVO toVO(SysCodeSequence sequence) {
        CodeSequenceVO base = toBaseVO(sequence);
        int seqLength = sequence.getSeqLength() == null ? 3 : sequence.getSeqLength();
        String nextNo = String.format("%0" + seqLength + "d", sequence.getCurrentNo() + 1);
        return new CodeSequenceVO(base.id(), base.bizType(), base.prefix(), base.period(),
            base.currentNo(), base.seqLength(), sequence.getPrefix() + sequence.getPeriod() + nextNo,
            base.updateTime());
    }

    /**
     * 序列实体 → VO 主体（MapStruct 生成，nextCodePreview 由 default toVO 补齐）。
     */
    @Mapping(target = "nextCodePreview", ignore = true)
    @Mapping(target = "updateTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    CodeSequenceVO toBaseVO(SysCodeSequence sequence);
}
