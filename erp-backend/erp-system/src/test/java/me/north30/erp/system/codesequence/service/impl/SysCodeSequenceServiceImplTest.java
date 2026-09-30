package me.north30.erp.system.codesequence.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.codesequence.CodeSequenceTestFactory;
import me.north30.erp.system.codesequence.converter.CodeSequenceConverter;
import me.north30.erp.system.codesequence.converter.CodeSequenceConverterImpl;
import me.north30.erp.system.codesequence.entity.SysCodeSequence;
import me.north30.erp.system.codesequence.mapper.SysCodeSequenceMapper;
import me.north30.erp.system.codesequence.vo.CodeSequenceResetVO;
import me.north30.erp.system.codesequence.vo.CodeSequenceVO;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.support.MpTableInfoInit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link SysCodeSequenceServiceImpl} 纯单元测试：分页与下一编号预览、流水重置的冲突/幂等/乐观锁分支。
 */
@ExtendWith(MockitoExtension.class)
class SysCodeSequenceServiceImplTest {

    @Mock
    private SysCodeSequenceMapper sysCodeSequenceMapper;

    @Spy
    private final CodeSequenceConverter codeSequenceConverter = new CodeSequenceConverterImpl();

    @InjectMocks
    private SysCodeSequenceServiceImpl service;

    @Captor
    private ArgumentCaptor<Page<SysCodeSequence>> pageCaptor;

    @Captor
    private ArgumentCaptor<SysCodeSequence> sequenceCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysCodeSequence.class);
    }

    @Nested
    @DisplayName("page：分页查询流水序列")
    class PageTest {

        @Test
        @DisplayName("分页查询流水序列，存在记录返回 VO 列表")
        void shouldReturnVOWithNextCodePreview_whenRecordsExist() {
            // Given：两条序列，seqLength 分别为 4 与 null（null 触发默认 3 位补零）
            SysCodeSequence s1 = CodeSequenceTestFactory.sequence(1L, "SALES_ORDER", "SO", "202609", 8, 4);
            s1.setUpdateTime(LocalDateTime.of(2026, 9, 28, 8, 0, 0));
            SysCodeSequence s2 = CodeSequenceTestFactory.sequence(2L, "PURCHASE_ORDER", "PO", "202609", 99, null);
            Page<SysCodeSequence> page = new Page<>(1, 20);
            page.setTotal(2);
            page.setRecords(List.of(s1, s2));
            given(sysCodeSequenceMapper.selectPage(any(), any())).willReturn(page);

            // When
            PageResult<CodeSequenceVO> result = service.page(
                CodeSequenceTestFactory.queryDTO(null, null, 1, 20));

            // Then：下一编号预览 = 前缀 + 期间 + 补零（currentNo+1）
            assertThat(result.total()).isEqualTo(2L);
            assertThat(result.pageNum()).isEqualTo(1L);
            assertThat(result.pageSize()).isEqualTo(20L);
            List<CodeSequenceVO> vos = result.list();
            assertThat(vos).hasSize(2);
            assertThat(vos.get(0).id()).isEqualTo(1L);
            assertThat(vos.get(0).bizType()).isEqualTo("SALES_ORDER");
            assertThat(vos.get(0).prefix()).isEqualTo("SO");
            assertThat(vos.get(0).period()).isEqualTo("202609");
            assertThat(vos.get(0).currentNo()).isEqualTo(8);
            assertThat(vos.get(0).seqLength()).isEqualTo(4);
            assertThat(vos.get(0).nextCodePreview()).isEqualTo("SO2026090009");
            assertThat(vos.get(0).updateTime()).isEqualTo("2026-09-28 08:00:00");
            assertThat(vos.get(1).seqLength()).isNull();
            assertThat(vos.get(1).nextCodePreview()).isEqualTo("PO202609100");
            assertThat(vos.get(1).updateTime()).isNull();
        }

        @Test
        @DisplayName("分页查询流水序列，每页条数超过上限抛出异常")
        void shouldThrow_whenPageSizeExceedsMax() {
            // Given：每页条数 201 超过上限 200
            var query = CodeSequenceTestFactory.queryDTO(null, null, 1, 201);

            // When + Then
            assertThatThrownBy(() -> service.page(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("pageSize 不能超过 200");
            verifyNoInteractions(sysCodeSequenceMapper);
        }

        @Test
        @DisplayName("分页查询流水序列，每页条数小于 1 时归位为默认值")
        void shouldNormalizePageSize_whenNonPositive() {
            // Given：每页条数小于 1 时归位为默认值
            Page<SysCodeSequence> page = new Page<>(1, 20);
            page.setTotal(0);
            page.setRecords(List.of());
            given(sysCodeSequenceMapper.selectPage(any(), any())).willReturn(page);

            // When
            service.page(CodeSequenceTestFactory.queryDTO(null, null, 1, 0));

            // Then
            verify(sysCodeSequenceMapper).selectPage(pageCaptor.capture(), any());
            assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
            assertThat(pageCaptor.getValue().getSize()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("reset：重置流水序列")
    class ResetTest {

        @Test
        @DisplayName("重置流水序列，目标值大于当前值更新当前编号")
        void shouldUpdateCurrentNo_whenTargetGreaterThanCurrent() {
            // Given：当前流水 5，向下重置至未使用区间起点 10
            SysCodeSequence sequence = CodeSequenceTestFactory.sequence(1L, "SALES_ORDER", "SO", "202609", 5, 4);
            given(sysCodeSequenceMapper.selectOne(any())).willReturn(sequence);
            given(sysCodeSequenceMapper.updateById(any(SysCodeSequence.class))).willReturn(1);
            var dto = CodeSequenceTestFactory.resetDTO("SALES_ORDER", "202609", 10, "期初建账调整");

            // When
            CodeSequenceResetVO vo = service.reset(dto);

            // Then
            assertThat(vo.bizType()).isEqualTo("SALES_ORDER");
            assertThat(vo.period()).isEqualTo("202609");
            assertThat(vo.currentNo()).isEqualTo(10);
            verify(sysCodeSequenceMapper).updateById(sequenceCaptor.capture());
            assertThat(sequenceCaptor.getValue().getCurrentNo()).isEqualTo(10);
        }

        @Test
        @DisplayName("重置流水序列，目标值与当前值相同（幂等跳过更新）")
        void shouldSkipUpdate_whenTargetEqualsCurrent() {
            // Given：目标值与当前值相同（幂等跳过更新）
            SysCodeSequence sequence = CodeSequenceTestFactory.sequence(1L, "SALES_ORDER", "SO", "202609", 5, 4);
            given(sysCodeSequenceMapper.selectOne(any())).willReturn(sequence);
            var dto = CodeSequenceTestFactory.resetDTO("SALES_ORDER", "202609", 5, "重复提交");

            // When
            CodeSequenceResetVO vo = service.reset(dto);

            // Then：不触发更新，直接返回当前值
            assertThat(vo.currentNo()).isEqualTo(5);
            verify(sysCodeSequenceMapper, never()).updateById(any(SysCodeSequence.class));
        }

        @Test
        @DisplayName("重置流水序列，目标值小于当前值抛出异常")
        void shouldThrow_whenSequenceNotFound() {
            // Given：单据类型 + 期间无对应序列
            given(sysCodeSequenceMapper.selectOne(any())).willReturn(null);
            var dto = CodeSequenceTestFactory.resetDTO("SALES_ORDER", "202609", 10, "期初建账调整");

            // When + Then
            assertThatThrownBy(() -> service.reset(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.SEQUENCE_NOT_FOUND.getCode()))
                .hasMessage("编号序列 SALES_ORDER/202609 不存在");
            verify(sysCodeSequenceMapper, never()).updateById(any(SysCodeSequence.class));
        }

        @Test
        @DisplayName("重置流水序列，目标值小于当前值抛出异常")
        void shouldThrow_whenTargetLessThanCurrent() {
            // Given：重置值 3 小于当前已用流水号 5
            SysCodeSequence sequence = CodeSequenceTestFactory.sequence(1L, "SALES_ORDER", "SO", "202609", 5, 4);
            given(sysCodeSequenceMapper.selectOne(any())).willReturn(sequence);
            var dto = CodeSequenceTestFactory.resetDTO("SALES_ORDER", "202609", 3, "误操作回退");

            // When + Then
            assertThatThrownBy(() -> service.reset(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.SEQUENCE_RESET_CONFLICT.getCode()))
                .hasMessage("流水号重置值不得小于当前已用流水号 5");
            verify(sysCodeSequenceMapper, never()).updateById(any(SysCodeSequence.class));
        }

        @Test
        @DisplayName("重置流水序列，重置操作触发乐观锁冲突，抛出异常")
        void shouldThrow_whenOptimisticLockConflict() {
            // Given：更新影响行数为 0（version 冲突）
            SysCodeSequence sequence = CodeSequenceTestFactory.sequence(1L, "SALES_ORDER", "SO", "202609", 5, 4);
            given(sysCodeSequenceMapper.selectOne(any())).willReturn(sequence);
            given(sysCodeSequenceMapper.updateById(any(SysCodeSequence.class))).willReturn(0);
            var dto = CodeSequenceTestFactory.resetDTO("SALES_ORDER", "202609", 10, "期初建账调整");

            // When + Then
            assertThatThrownBy(() -> service.reset(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()))
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }
}
