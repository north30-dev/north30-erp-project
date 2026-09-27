package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.core.dto.CodeSequenceQueryDTO;
import me.north30.erp.system.core.dto.CodeSequenceResetDTO;
import me.north30.erp.system.core.entity.SysCodeSequence;
import me.north30.erp.system.core.enums.SystemManageErrorCode;
import me.north30.erp.system.core.mapper.SysCodeSequenceMapper;
import me.north30.erp.system.core.service.ISysCodeSequenceService;
import me.north30.erp.system.core.util.DateTimeFormatUtil;
import me.north30.erp.system.core.vo.CodeSequenceResetVO;
import me.north30.erp.system.core.vo.CodeSequenceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 单据编号序列服务实现。
 * <p>仅提供分页查询与流水重置；发号逻辑由业务模块实现。重置仅允许"向下重置至未使用区间"，
 * 目标流水号不得小于当前已用值；目标值相同时幂等跳过更新。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysCodeSequenceServiceImpl implements ISysCodeSequenceService {

    private final SysCodeSequenceMapper sysCodeSequenceMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<CodeSequenceVO> page(CodeSequenceQueryDTO query) {
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        LambdaQueryWrapper<SysCodeSequence> wrapper = new LambdaQueryWrapper<SysCodeSequence>()
            .eq(StringUtils.hasText(query.getBizType()), SysCodeSequence::getBizType, query.getBizType())
            .eq(StringUtils.hasText(query.getPeriod()), SysCodeSequence::getPeriod, query.getPeriod())
            .orderByAsc(SysCodeSequence::getBizType)
            .orderByDesc(SysCodeSequence::getPeriod);
        Page<SysCodeSequence> page = sysCodeSequenceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<CodeSequenceVO> vos = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public CodeSequenceResetVO reset(CodeSequenceResetDTO dto) {
        SysCodeSequence sequence = sysCodeSequenceMapper.selectOne(new LambdaQueryWrapper<SysCodeSequence>()
            .eq(SysCodeSequence::getBizType, dto.bizType())
            .eq(SysCodeSequence::getPeriod, dto.period()));
        if (sequence == null) {
            throw new BusinessException(SystemManageErrorCode.SEQUENCE_NOT_FOUND,
                "编号序列 " + dto.bizType() + "/" + dto.period() + " 不存在");
        }
        if (dto.currentNo() < sequence.getCurrentNo()) {
            throw new BusinessException(SystemManageErrorCode.SEQUENCE_RESET_CONFLICT,
                "流水号重置值不得小于当前已用流水号 " + sequence.getCurrentNo());
        }
        // 幂等：目标值与当前值相同则跳过更新
        if (!dto.currentNo().equals(sequence.getCurrentNo())) {
            sequence.setCurrentNo(dto.currentNo());
            if (sysCodeSequenceMapper.updateById(sequence) == 0) {
                throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
            }
            log.info("编号序列已重置 | bizType: {} | period: {} | currentNo: {} | 原因: {}",
                dto.bizType(), dto.period(), dto.currentNo(), dto.reason());
        }
        return new CodeSequenceResetVO(sequence.getBizType(), sequence.getPeriod(), sequence.getCurrentNo());
    }

    /**
     * Entity → VO 转换：下一编号预览 = prefix + period + 补零（currentNo+1）。
     */
    private CodeSequenceVO toVO(SysCodeSequence sequence) {
        int seqLength = sequence.getSeqLength() == null ? 3 : sequence.getSeqLength();
        String nextNo = String.format("%0" + seqLength + "d", sequence.getCurrentNo() + 1);
        return new CodeSequenceVO(sequence.getId(), sequence.getBizType(), sequence.getPrefix(),
            sequence.getPeriod(), sequence.getCurrentNo(), sequence.getSeqLength(),
            sequence.getPrefix() + sequence.getPeriod() + nextNo,
            DateTimeFormatUtil.format(sequence.getUpdateTime()));
    }

    private long normalizePageNum(long pageNum) {
        return pageNum <= 0 ? PageConstants.DEFAULT_PAGE_NUM : pageNum;
    }

    private long normalizePageSize(long pageSize) {
        if (pageSize <= 0) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "pageSize 不能超过 " + PageConstants.MAX_PAGE_SIZE);
        }
        return pageSize;
    }
}
