package me.north30.erp.system.codesequence.service.impl;

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
import me.north30.erp.system.codesequence.converter.CodeSequenceConverter;
import me.north30.erp.system.codesequence.dto.CodeSequenceQueryDTO;
import me.north30.erp.system.codesequence.dto.CodeSequenceResetDTO;
import me.north30.erp.system.codesequence.entity.SysCodeSequence;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.codesequence.mapper.SysCodeSequenceMapper;
import me.north30.erp.system.codesequence.service.SysCodeSequenceService;
import me.north30.erp.system.codesequence.vo.CodeSequenceResetVO;
import me.north30.erp.system.codesequence.vo.CodeSequenceVO;
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
public class SysCodeSequenceServiceImpl implements SysCodeSequenceService {

    private final SysCodeSequenceMapper sysCodeSequenceMapper;
    private final CodeSequenceConverter codeSequenceConverter;

    @Override
    @Transactional(readOnly = true)
    public PageResult<CodeSequenceVO> page(CodeSequenceQueryDTO query) {
        long pageNum = normalizePageNum(query.pageNum());
        long pageSize = normalizePageSize(query.pageSize());
        LambdaQueryWrapper<SysCodeSequence> wrapper = new LambdaQueryWrapper<SysCodeSequence>()
            .eq(StringUtils.hasText(query.bizType()), SysCodeSequence::getBizType, query.bizType())
            .eq(StringUtils.hasText(query.period()), SysCodeSequence::getPeriod, query.period())
            .orderByAsc(SysCodeSequence::getBizType)
            .orderByDesc(SysCodeSequence::getPeriod);
        Page<SysCodeSequence> page = sysCodeSequenceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<CodeSequenceVO> vos = page.getRecords().stream().map(codeSequenceConverter::toVO).toList();
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

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? PageConstants.DEFAULT_PAGE_NUM : pageNum;
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "pageSize 不能超过 " + PageConstants.MAX_PAGE_SIZE);
        }
        return pageSize;
    }
}
