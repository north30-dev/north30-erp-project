package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.core.dto.AuditLogQueryDTO;
import me.north30.erp.system.core.dto.AuditLogQueryParam;
import me.north30.erp.system.core.dto.LoginLogQueryDTO;
import me.north30.erp.system.core.entity.SysLoginLog;
import me.north30.erp.system.core.mapper.AuditLogQueryMapper;
import me.north30.erp.system.core.mapper.SysLoginLogMapper;
import me.north30.erp.system.core.vo.AuditLogVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysLogQueryServiceImpl 单元测试：日志分页筛选条件拼接、时间区间左闭右开解析、
 * 详情 diffFields 派生、不存在异常与分页参数归一化。
 */
@ExtendWith(MockitoExtension.class)
class SysLogQueryServiceImplTest {

    @Mock
    private AuditLogQueryMapper auditLogQueryMapper;
    @Mock
    private SysLoginLogMapper sysLoginLogMapper;

    private SysLogQueryServiceImpl logQueryService;

    @BeforeAll
    static void initTableInfo() {
        // LambdaQueryWrapper 解析 SysLoginLog::getXxx 需要实体元数据缓存（无 Spring 容器时手工初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, SysLoginLog.class);
    }

    @BeforeEach
    void setUp() {
        logQueryService = new SysLogQueryServiceImpl(auditLogQueryMapper, sysLoginLogMapper);
    }

    @Test
    @DisplayName("审计日志分页：筛选条件与时间区间正确传递给 Mapper")
    void pageAuditLogsBuildsCorrectParam() {
        AuditLogQueryDTO query = new AuditLogQueryDTO();
        query.setPageNum(2);
        query.setPageSize(50);
        query.setBizCode("SO2026");
        query.setModule("SALES");
        query.setBizType("SALES_ORDER");
        query.setOperateType("APPROVE");
        query.setOperateBy("admin");
        query.setResultStatus(1);
        query.setStartTime("2026-08-01 00:00:00");
        query.setEndTime("2026-09-01");
        mockAuditLogPage(1);

        logQueryService.pageAuditLogs(query);

        ArgumentCaptor<AuditLogQueryParam> paramCaptor = ArgumentCaptor.forClass(AuditLogQueryParam.class);
        ArgumentCaptor<Page<AuditLogVO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(auditLogQueryMapper).selectAuditLogPage(pageCaptor.capture(), paramCaptor.capture());
        AuditLogQueryParam param = paramCaptor.getValue();
        assertEquals("SO2026", param.getBizCode());
        assertEquals("SALES", param.getModule());
        assertEquals("SALES_ORDER", param.getBizType());
        assertEquals("APPROVE", param.getOperateType());
        assertEquals("admin", param.getOperateBy());
        assertEquals(1, param.getResultStatus());
        // 左闭：起点为 2026-08-01T00:00；右开：纯日期终点展开为次日零点
        assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0, 0), param.getTimeStart());
        assertEquals(LocalDate.of(2026, 9, 2).atStartOfDay(), param.getTimeEnd());
        assertEquals(2, pageCaptor.getValue().getCurrent());
        assertEquals(50, pageCaptor.getValue().getSize());
    }

    @Test
    @DisplayName("审计日志分页：页码页缺省归一化为默认值")
    void pageAuditLogsWithDefaultPageParams() {
        AuditLogQueryDTO query = new AuditLogQueryDTO();
        query.setBizCode("PO2026");
        mockAuditLogPage(0);

        PageResult<AuditLogVO> result = logQueryService.pageAuditLogs(query);

        assertEquals(1, result.pageNum());
        assertEquals(20, result.pageSize());
        ArgumentCaptor<Page<AuditLogVO>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(auditLogQueryMapper).selectAuditLogPage(pageCaptor.capture(), any(AuditLogQueryParam.class));
        assertEquals(1, pageCaptor.getValue().getCurrent());
        assertEquals(20, pageCaptor.getValue().getSize());
    }

    @Test
    @DisplayName("审计日志分页：时间参数格式非法抛 10001")
    void pageAuditLogsWithInvalidTime() {
        AuditLogQueryDTO query = new AuditLogQueryDTO();
        query.setStartTime("2026/08/01");

        BusinessException exception = assertThrows(BusinessException.class,
            () -> logQueryService.pageAuditLogs(query));
        assertEquals(10001, exception.getCode());
    }

    @Test
    @DisplayName("审计日志分页：pageSize 超过 200 抛 10001")
    void pageAuditLogsWithOversizePageSize() {
        AuditLogQueryDTO query = new AuditLogQueryDTO();
        query.setPageSize(201);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> logQueryService.pageAuditLogs(query));
        assertEquals(10001, exception.getCode());
    }

    @Test
    @DisplayName("审计日志详情：不存在抛 18037")
    void getAuditLogDetailNotFound() {
        when(auditLogQueryMapper.selectAuditLogById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> logQueryService.getAuditLogDetail(99L));
        assertEquals(18037, exception.getCode());
    }

    @Test
    @DisplayName("审计日志详情：diffFields 派生对比 before/after JSON 键值差异")
    void getAuditLogDetailResolvesDiffFields() {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(1L);
        vo.setBeforeJson("{\"amount\":100,\"status\":\"DRAFT\",\"note\":null}");
        vo.setAfterJson("{\"amount\":200,\"status\":\"DRAFT\",\"approved\":true}");
        when(auditLogQueryMapper.selectAuditLogById(1L)).thenReturn(vo);

        AuditLogVO detail = logQueryService.getAuditLogDetail(1L);

        // amount 变化、approved 新增；status 未变不列入；note 在 before 中为 null，与缺失等价不列入
        List<String> diffFields = detail.getDiffFields();
        assertNotNull(diffFields);
        assertEquals(List.of("amount", "approved"), diffFields);
    }

    @Test
    @DisplayName("审计日志详情：无变更 JSON 时 diffFields 为空列表")
    void getAuditLogDetailWithoutJson() {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(2L);
        when(auditLogQueryMapper.selectAuditLogById(2L)).thenReturn(vo);

        AuditLogVO detail = logQueryService.getAuditLogDetail(2L);

        assertTrue(detail.getDiffFields().isEmpty());
    }

    @Test
    @DisplayName("登录日志分页：筛选条件拼接正确（模糊/等值/左闭右开）")
    void pageLoginLogsBuildsCorrectWrapper() {
        LoginLogQueryDTO query = new LoginLogQueryDTO();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setUsername("admin");
        query.setLoginType(4);
        query.setResultStatus(0);
        query.setStartTime("2026-08-01");
        query.setEndTime("2026-08-02 00:00:00");
        mockLoginLogPage(2);

        PageResult<?> result = logQueryService.pageLoginLogs(query);

        assertEquals(2, result.total());
        ArgumentCaptor<LambdaQueryWrapper<SysLoginLog>> wrapperCaptor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(sysLoginLogMapper).selectPage(any(), wrapperCaptor.capture());
        LambdaQueryWrapper<SysLoginLog> wrapper = wrapperCaptor.getValue();
        // 先触发 SQL 片段求值：MP 3.5.x 的 formatParam 惰性执行，求值前 paramNameValuePairs 为空
        String sqlSegment = wrapper.getSqlSegment();
        // 值绑定：模糊用户名、事件类型、结果状态与解析后的时间区间
        assertTrue(wrapper.getParamNameValuePairs().containsValue("%admin%"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(4));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(0));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(LocalDate.of(2026, 8, 1).atStartOfDay()));
        // 右开区间：终点精确时间不再补次日
        assertTrue(wrapper.getParamNameValuePairs().containsValue(LocalDateTime.of(2026, 8, 2, 0, 0, 0)));
        // SQL 片段：时间条件为左闭（>=）右开（<），且按事件时间倒序
        assertTrue(sqlSegment.contains(">="));
        assertTrue(sqlSegment.contains("<"));
        assertTrue(sqlSegment.contains("LIKE"));
        assertTrue(sqlSegment.contains("ORDER BY"));
    }

    @Test
    @DisplayName("登录日志分页：pageSize 超过 200 抛 10001")
    void pageLoginLogsWithOversizePageSize() {
        LoginLogQueryDTO query = new LoginLogQueryDTO();
        query.setPageSize(500);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> logQueryService.pageLoginLogs(query));
        assertEquals(10001, exception.getCode());
    }

    private void mockAuditLogPage(long total) {
        when(auditLogQueryMapper.selectAuditLogPage(any(), any())).thenAnswer(invocation -> {
            Page<AuditLogVO> page = invocation.getArgument(0);
            page.setTotal(total);
            page.setRecords(List.of());
            return page;
        });
    }

    private void mockLoginLogPage(long total) {
        when(sysLoginLogMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<SysLoginLog> page = invocation.getArgument(0);
            page.setTotal(total);
            page.setRecords(List.of());
            return page;
        });
    }
}
