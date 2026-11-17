package me.north30.erp.system.codesequence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import me.north30.erp.system.codesequence.entity.SysCodeSequence;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 单据编号序列表 Mapper。
 */
public interface SysCodeSequenceMapper extends BaseMapper<SysCodeSequence> {

    /**
     * 原子取号：行存在且流水未达上限时行锁串行递增 current_no，RETURNING 返回本次占用号。
     * <p>行不存在或流水已用尽（current_no ≥ maxNo）时更新 0 行、返回 null；
     * version 同步 +1 保持乐观锁语义；update_by/update_time 手工填充（绕过 MP 自动填充）。</p>
     *
     * @param bizType  业务类型（取号键之一）
     * @param period   期间（取号键之二，唯一键 biz_type + period）
     * @param maxNo    流水上限（达到即视为用尽）
     * @param operator 当前操作人（update_by 填充值）
     * @return 本次占用的流水号；无可用行或已用尽时返回 null
     */
    @Select("UPDATE sys_code_sequence "
        + "SET current_no = current_no + 1, version = version + 1, update_by = #{operator}, update_time = NOW() "
        + "WHERE biz_type = #{bizType} AND period = #{period} AND is_deleted = 0 AND current_no < #{maxNo} "
        + "RETURNING current_no")
    Integer allocateCurrentNo(@Param("bizType") String bizType,
                              @Param("period") String period,
                              @Param("maxNo") int maxNo,
                              @Param("operator") String operator);

    /**
     * 插入种子行（首个流水号占用）：并发首取时经部分唯一索引 uk_sys_code_sequence 幂等去重（DO NOTHING 不抛冲突）。
     * <p>命中冲突（并发方已插入）返回 0，由调用方循环重试原子递增。</p>
     *
     * @param bizType   业务类型
     * @param prefix    编号前缀
     * @param period    期间（唯一键 biz_type + period）
     * @param currentNo 初始流水号
     * @param seqLength 流水位数
     * @param operator  当前操作人（create_by/update_by 填充值）
     * @return 实际插入行数（1 插入成功；0 唯一键冲突被幂等跳过）
     */
    @Insert("INSERT INTO sys_code_sequence "
        + "(biz_type, prefix, period, current_no, seq_length, create_by, create_time, update_by, update_time, version, is_deleted) "
        + "VALUES (#{bizType}, #{prefix}, #{period}, #{currentNo}, #{seqLength}, #{operator}, NOW(), #{operator}, NOW(), 0, 0) "
        + "ON CONFLICT (biz_type, period) WHERE is_deleted = 0 DO NOTHING")
    int insertSeedRow(@Param("bizType") String bizType,
                      @Param("prefix") String prefix,
                      @Param("period") String period,
                      @Param("currentNo") int currentNo,
                      @Param("seqLength") int seqLength,
                      @Param("operator") String operator);
}
