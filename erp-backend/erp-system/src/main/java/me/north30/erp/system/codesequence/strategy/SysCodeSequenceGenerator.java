package me.north30.erp.system.codesequence.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.security.CurrentUserProvider;
import me.north30.erp.common.spi.CodeGenerator;
import me.north30.erp.system.codesequence.entity.SysCodeSequence;
import me.north30.erp.system.codesequence.mapper.SysCodeSequenceMapper;
import me.north30.erp.system.common.enums.SystemErrorCode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 编号序列原子发号策略：实现 erp-common {@link CodeGenerator} SPI（既有
 * {@code SysCodeSequenceService} 仅含查询/重置，发号能力由本策略新增承载）。
 * <p>并发方案（数据库行级并发，无应用内锁）：</p>
 * <ol>
 *     <li>原子递增：{@code UPDATE ... SET current_no = current_no + 1 ... RETURNING current_no}，
 *     行锁串行化并发取号，RETURNING 保证各事务拿到互不重复的占用号；</li>
 *     <li>行不存在：插入种子行（首号占用），经部分唯一索引 biz_type + period 幂等（DO NOTHING 不抛冲突，
 *     避免唯一键异常污染外层事务）；种子竞争失败则循环重试递增；</li>
 *     <li>流水用尽：current_no 达上限仍取号抛 {@link SystemErrorCode#SEQUENCE_EXHAUSTED}（system 段 18044）；
 *     base 物料域策略可按 code 转译为自身语义码（11014 物料编码流水已用尽）后透出。</li>
 * </ol>
 * <p>取号约定：以 (bizType, period = prefix) 为取号键按前缀隔离递增（物料编码 bizType=material_code、
 * prefix=分类编码前 4 位）；本 SPI 固定返回 6 位流水。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysCodeSequenceGenerator implements CodeGenerator {

    /** 6 位流水上限（达到即视为用尽） */
    private static final int MAX_SEQUENCE_NO = 999_999;

    /** 流水位数（sys_code_sequence.seq_length，本 SPI 统一 6 位） */
    private static final int SEQ_LENGTH = 6;

    /** 首取种子竞争重试上限（仅覆盖"种子行未提交可见"窗口，超限按并发冲突失败可重试） */
    private static final int MAX_RETRY = 3;

    /** 无登录上下文时的兜底操作人（与 MyMetaObjectHandler 口径一致） */
    private static final String SYSTEM_USER = "system";

    private final SysCodeSequenceMapper sysCodeSequenceMapper;
    private final ObjectProvider<CurrentUserProvider> currentUserProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generate(String bizType, String prefix) {
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(prefix)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "取号参数不能为空（bizType/prefix）");
        }
        String operator = resolveOperator();
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            // ① 行存在且未用尽：原子递增并返回本次占用号
            Integer allocated = sysCodeSequenceMapper.allocateCurrentNo(bizType, prefix, MAX_SEQUENCE_NO, operator);
            if (allocated != null) {
                return prefix + String.format("%06d", allocated);
            }
            // ② 递增 0 行：行已存在且用尽 → 抛流水用尽；行存在但未用尽 → 种子行刚可见，回到 ① 递增
            SysCodeSequence sequence = sysCodeSequenceMapper.selectOne(new LambdaQueryWrapper<SysCodeSequence>()
                .eq(SysCodeSequence::getBizType, bizType)
                .eq(SysCodeSequence::getPeriod, prefix));
            if (sequence != null) {
                if (sequence.getCurrentNo() != null && sequence.getCurrentNo() >= MAX_SEQUENCE_NO) {
                    throw new BusinessException(SystemErrorCode.SEQUENCE_EXHAUSTED,
                        "编号序列 " + bizType + "/" + prefix + " 流水已用尽（上限 " + MAX_SEQUENCE_NO + "）");
                }
                continue;
            }
            // ③ 行不存在：插入种子行占住首号；命中并发种子冲突幂等跳过（返回 0）后重试
            int inserted = sysCodeSequenceMapper.insertSeedRow(bizType, prefix, prefix, 1, SEQ_LENGTH, operator);
            if (inserted == 1) {
                return prefix + String.format("%06d", 1);
            }
        }
        throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT,
            "编号获取并发冲突，请重试（bizType: " + bizType + "，prefix: " + prefix + "）");
    }

    /**
     * 解析当前操作人：CurrentUserProvider 返回用户名；无实现 Bean 或无登录上下文时降级为 "system"。
     */
    private String resolveOperator() {
        CurrentUserProvider provider = currentUserProvider.getIfAvailable();
        if (provider == null) {
            return SYSTEM_USER;
        }
        String username = provider.getCurrentUsername();
        return username != null ? username : SYSTEM_USER;
    }
}
