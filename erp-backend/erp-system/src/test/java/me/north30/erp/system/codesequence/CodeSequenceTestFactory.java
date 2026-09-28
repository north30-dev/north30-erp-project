package me.north30.erp.system.codesequence;

import me.north30.erp.system.codesequence.dto.CodeSequenceQueryDTO;
import me.north30.erp.system.codesequence.dto.CodeSequenceResetDTO;
import me.north30.erp.system.codesequence.entity.SysCodeSequence;

/**
 * 编号序列域测试数据静态工厂：集中构造实体与 DTO，避免测试方法内堆砌字段。
 */
public final class CodeSequenceTestFactory {

    private CodeSequenceTestFactory() {
    }

    /**
     * 构建编号序列实体（更新时间为空，由用例按需补充）。
     */
    public static SysCodeSequence sequence(Long id, String bizType, String prefix, String period,
                                           Integer currentNo, Integer seqLength) {
        SysCodeSequence sequence = new SysCodeSequence();
        sequence.setId(id);
        sequence.setBizType(bizType);
        sequence.setPrefix(prefix);
        sequence.setPeriod(period);
        sequence.setCurrentNo(currentNo);
        sequence.setSeqLength(seqLength);
        return sequence;
    }

    /**
     * 重置编号流水请求 DTO。
     */
    public static CodeSequenceResetDTO resetDTO(String bizType, String period, Integer currentNo, String reason) {
        return new CodeSequenceResetDTO(bizType, period, currentNo, reason);
    }

    /**
     * 编号序列分页查询参数。
     */
    public static CodeSequenceQueryDTO queryDTO(String bizType, String period, long pageNum, long pageSize) {
        CodeSequenceQueryDTO query = new CodeSequenceQueryDTO();
        query.setBizType(bizType);
        query.setPeriod(period);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return query;
    }
}
