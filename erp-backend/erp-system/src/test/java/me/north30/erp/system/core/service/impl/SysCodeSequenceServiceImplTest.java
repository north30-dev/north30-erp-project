package me.north30.erp.system.core.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.core.dto.CodeSequenceResetDTO;
import me.north30.erp.system.core.vo.CodeSequenceResetVO;
import me.north30.erp.system.core.entity.SysCodeSequence;
import me.north30.erp.system.core.mapper.SysCodeSequenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysCodeSequenceServiceImpl 单元测试：覆盖序列不存在、向下重置越界被拒、重置成功与幂等重置。
 */
@ExtendWith(MockitoExtension.class)
class SysCodeSequenceServiceImplTest {

    @Mock
    private SysCodeSequenceMapper sysCodeSequenceMapper;

    private SysCodeSequenceServiceImpl sequenceService;

    @BeforeEach
    void setUp() {
        sequenceService = new SysCodeSequenceServiceImpl(sysCodeSequenceMapper);
    }

    private SysCodeSequence sequence(int currentNo) {
        SysCodeSequence sequence = new SysCodeSequence();
        sequence.setId(1L);
        sequence.setBizType("SALES_ORDER");
        sequence.setPrefix("SO");
        sequence.setPeriod("202609");
        sequence.setCurrentNo(currentNo);
        sequence.setSeqLength(3);
        sequence.setVersion(0);
        return sequence;
    }

    private CodeSequenceResetDTO resetDTO(int currentNo) {
        return new CodeSequenceResetDTO("SALES_ORDER", "202609", currentNo, "期初建账调整");
    }

    @Test
    @DisplayName("重置失败：序列不存在抛 18035")
    void resetWithMissingSequence() {
        when(sysCodeSequenceMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> sequenceService.reset(resetDTO(0)));

        assertEquals(18035, ex.getCode());
    }

    @Test
    @DisplayName("重置失败：目标值小于当前已用流水号抛 18036")
    void resetWithSmallerValue() {
        when(sysCodeSequenceMapper.selectOne(any())).thenReturn(sequence(18));

        BusinessException ex = assertThrows(BusinessException.class, () -> sequenceService.reset(resetDTO(10)));

        assertEquals(18036, ex.getCode());
        verify(sysCodeSequenceMapper, never()).updateById(any(SysCodeSequence.class));
    }

    @Test
    @DisplayName("重置成功：目标值大于当前值时更新流水")
    void resetSuccess() {
        when(sysCodeSequenceMapper.selectOne(any())).thenReturn(sequence(18));
        when(sysCodeSequenceMapper.updateById(any(SysCodeSequence.class))).thenReturn(1);

        CodeSequenceResetVO vo = sequenceService.reset(resetDTO(30));

        assertEquals("SALES_ORDER", vo.bizType());
        assertEquals("202609", vo.period());
        assertEquals(30, vo.currentNo());
        verify(sysCodeSequenceMapper).updateById(any(SysCodeSequence.class));
    }

    @Test
    @DisplayName("重置幂等：目标值等于当前值时跳过更新直接成功")
    void resetIdempotent() {
        when(sysCodeSequenceMapper.selectOne(any())).thenReturn(sequence(18));

        CodeSequenceResetVO vo = sequenceService.reset(resetDTO(18));

        assertEquals(18, vo.currentNo());
        verify(sysCodeSequenceMapper, never()).updateById(any(SysCodeSequence.class));
    }

    @Test
    @DisplayName("重置幂等：连续重置到同一目标值，第二次不再触发更新")
    void resetRepeatedToSameTarget() {
        SysCodeSequence seq = sequence(18);
        when(sysCodeSequenceMapper.selectOne(any())).thenReturn(seq);
        when(sysCodeSequenceMapper.updateById(any(SysCodeSequence.class))).thenReturn(1);

        sequenceService.reset(resetDTO(30));
        // 第一次重置后实体内存值已变为 30，模拟再次重置到 30
        CodeSequenceResetVO second = sequenceService.reset(resetDTO(30));

        assertEquals(30, second.currentNo());
        verify(sysCodeSequenceMapper, times(1)).updateById(any(SysCodeSequence.class));
    }
}
