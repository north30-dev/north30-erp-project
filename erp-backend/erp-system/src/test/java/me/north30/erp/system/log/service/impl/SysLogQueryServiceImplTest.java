package me.north30.erp.system.log.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.log.LogTestFactory;
import me.north30.erp.system.log.dto.AuditLogQueryParam;
import me.north30.erp.system.log.entity.SysLoginLog;
import me.north30.erp.system.log.mapper.AuditLogQueryMapper;
import me.north30.erp.system.log.mapper.SysLoginLogMapper;
import me.north30.erp.system.log.vo.AuditLogVO;
import me.north30.erp.system.log.vo.LoginLogVO;
import me.north30.erp.system.support.MpTableInfoInit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link SysLogQueryServiceImpl} 纯单元测试：审计日志分页参数解析与详情 diff、登录日志分页。
 */
@ExtendWith(MockitoExtension.class)
class SysLogQueryServiceImplTest {

    @Mock
    private AuditLogQueryMapper auditLogQueryMapper;

    @Mock
    private SysLoginLogMapper sysLoginLogMapper;

    @InjectMocks
    private SysLogQueryServiceImpl service;

    @Captor
    private ArgumentCaptor<Page<AuditLogVO>> auditPageCaptor;

    @Captor
    private ArgumentCaptor<AuditLogQueryParam> auditParamCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        // 登录日志分页内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysLoginLog.class);
    }

    @Nested
    @DisplayName("审计日志分页查询测试")
    class PageAuditLogsTest {

        @Test   
        @DisplayName("查询审计日志时，根据日期范围解析参数")
        void shouldMapFiltersAndExpandDateRange_whenQueryByDate() {
            // Given：日期字符串区间（yyyy-MM-dd），起点左闭、终点右开（+1 天）
            AuditLogVO vo = LogTestFactory.auditLogVO(1L, null, null);
            given(auditLogQueryMapper.selectAuditLogPage(any(), any())).willAnswer(invocation -> {
                Page<AuditLogVO> page = invocation.getArgument(0);
                page.setTotal(1);
                page.setRecords(List.of(vo));
                return page;
            });
            var query = LogTestFactory.auditLogQueryDTO(2, 50, "PO2026", "PURCHASE", "PURCHASE_ORDER",
                "UPDATE", "admin", 0, "2026-09-01", "2026-09-28");

            // When
            PageResult<AuditLogVO> result = service.pageAuditLogs(query);

            // Then：分页字段透传，过滤字段与时间边界正确展开
            assertThat(result.total()).isEqualTo(1L);
            assertThat(result.pageNum()).isEqualTo(2L);
            assertThat(result.pageSize()).isEqualTo(50L);
            assertThat(result.pages()).isEqualTo(1L);
            assertThat(result.list()).containsExactly(vo);
            verify(auditLogQueryMapper).selectAuditLogPage(auditPageCaptor.capture(), auditParamCaptor.capture());
            assertThat(auditPageCaptor.getValue().getCurrent()).isEqualTo(2L);
            assertThat(auditPageCaptor.getValue().getSize()).isEqualTo(50L);
            AuditLogQueryParam param = auditParamCaptor.getValue();
            assertThat(param.getBizCode()).isEqualTo("PO2026");
            assertThat(param.getModule()).isEqualTo("PURCHASE");
            assertThat(param.getBizType()).isEqualTo("PURCHASE_ORDER");
            assertThat(param.getOperateType()).isEqualTo("UPDATE");
            assertThat(param.getOperateBy()).isEqualTo("admin");
            assertThat(param.getResultStatus()).isZero();
            assertThat(param.getTimeStart()).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0, 0));
            assertThat(param.getTimeEnd()).isEqualTo(LocalDateTime.of(2026, 9, 29, 0, 0, 0));
        }

        @Test   
        @DisplayName("查询审计日志时，根据完整时间戳解析起点时间")  
        void shouldParseDateTime_whenFullTimestampGiven() {
            // Given：起点为完整时间戳（yyyy-MM-dd HH:mm:ss），终点为空
            given(auditLogQueryMapper.selectAuditLogPage(any(), any())).willAnswer(invocation -> {
                Page<AuditLogVO> page = invocation.getArgument(0);
                page.setTotal(0);
                page.setRecords(List.of());
                return page;
            });
            var query = LogTestFactory.auditLogQueryDTO(1, 20, null, null, null, null, null, null,
                "2026-09-01 08:30:15", null);

            // When
            PageResult<AuditLogVO> result = service.pageAuditLogs(query);

            // Then
            assertThat(result.list()).isEmpty();
            assertThat(result.total()).isZero();
            verify(auditLogQueryMapper).selectAuditLogPage(any(), auditParamCaptor.capture());
            assertThat(auditParamCaptor.getValue().getTimeStart())
                .isEqualTo(LocalDateTime.of(2026, 9, 1, 8, 30, 15));
            assertThat(auditParamCaptor.getValue().getTimeEnd()).isNull();
        }

        @Test   
        @DisplayName("分页查询审计日志时，每页条数超过上限 200 抛出异常")
        void shouldThrow_whenPageSizeExceedsMax() {
            // Given：每页条数 201 超过上限 200
            var query = LogTestFactory.auditLogQueryDTO(1, 201, null, null, null, null, null, null, null, null);

            // When + Then
            assertThatThrownBy(() -> service.pageAuditLogs(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("每页条数须在 1-200 之间");
            verifyNoInteractions(auditLogQueryMapper);
        }

        @Test   
        @DisplayName("查询审计日志时，时间格式既非 yyyy-MM-dd 也非 yyyy-MM-dd HH:mm:ss 抛出异常")
        void shouldThrow_whenTimeFormatInvalid() {
            // Given：时间格式既非 yyyy-MM-dd 也非 yyyy-MM-dd HH:mm:ss
            var query = LogTestFactory.auditLogQueryDTO(1, 20, null, null, null, null, null, null,
                "2026/09/01", null);

            // When + Then
            assertThatThrownBy(() -> service.pageAuditLogs(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("时间参数格式非法，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
            verifyNoInteractions(auditLogQueryMapper);
        }
    }

    @Nested
    @DisplayName("审计日志详情查询测试")
    class GetAuditLogDetailTest {

        @Test   
        @DisplayName("查询审计日志详情时，根据变更字段计算 diffFields")
        void shouldReturnDetailWithDiffFields_whenJsonDiffers() {
            // Given：变更前（name 修改、note 删除）与变更后（extra 新增）
            AuditLogVO vo = LogTestFactory.auditLogVO(1L,
                "{\"name\":\"旧\",\"qty\":1,\"note\":\"x\"}",
                "{\"name\":\"新\",\"qty\":1,\"extra\":true}");
            given(auditLogQueryMapper.selectAuditLogById(1L)).willReturn(vo);

            // When
            AuditLogVO detail = service.getAuditLogDetail(1L);

            // Then：diffFields 按字典序输出变更字段
            assertThat(detail).isSameAs(vo);
            assertThat(detail.getDiffFields()).containsExactly("extra", "name", "note");
        }

        @Test   
        @DisplayName("查询审计日志详情时，变更前后 JSON 均为空/空白返回空差异")
        void shouldReturnEmptyDiff_whenBothJsonBlank() {
            // Given：变更前后 JSON 均为空/空白
            AuditLogVO vo = LogTestFactory.auditLogVO(1L, null, "  ");
            given(auditLogQueryMapper.selectAuditLogById(1L)).willReturn(vo);

            // When
            AuditLogVO detail = service.getAuditLogDetail(1L);

            // Then：diffFields 为空列表
            assertThat(detail.getDiffFields()).isEmpty();
        }

        @Test   
        @DisplayName("查询审计日志详情时，解析变更字段时忽略无效 JSON 不报错")
        void shouldSkipInvalidJson_whenParseFails() {
            // Given：beforeJson 非法（解析失败仅告警不阻断），afterJson 合法
            AuditLogVO vo = LogTestFactory.auditLogVO(1L, "not-json", "{\"a\":1}");
            given(auditLogQueryMapper.selectAuditLogById(1L)).willReturn(vo);

            // When
            AuditLogVO detail = service.getAuditLogDetail(1L);

            // Then：按"before 缺失"计算差异
            assertThat(detail.getDiffFields()).containsExactly("a");
        }

        @Test   
        @DisplayName("查询审计日志详情时，变更后 JSON 为数组（非对象）视为缺失字段")
        void shouldTreatNonObjectJsonAsMissing() {
            // Given：afterJson 为 JSON 数组（非对象，视为缺失）
            AuditLogVO vo = LogTestFactory.auditLogVO(1L, "{\"a\":1}", "[1,2]");
            given(auditLogQueryMapper.selectAuditLogById(1L)).willReturn(vo);

            // When
            AuditLogVO detail = service.getAuditLogDetail(1L);

            // Then
            assertThat(detail.getDiffFields()).containsExactly("a");
        }

        @Test   
        @DisplayName("查询审计日志详情时，null 值与缺失字段视为等价（b 未变化、c 与缺失等价）")
        void shouldTreatNullValueEquivalentToMissingField() {
            // Given：null 值与缺失字段视为等价（b 未变化、c 与缺失等价）
            AuditLogVO vo = LogTestFactory.auditLogVO(1L,
                "{\"a\":1,\"b\":2}",
                "{\"a\":null,\"b\":2,\"c\":null}");
            given(auditLogQueryMapper.selectAuditLogById(1L)).willReturn(vo);

            // When
            AuditLogVO detail = service.getAuditLogDetail(1L);

            // Then：仅 a（值改为 null）视为变更字段
            assertThat(detail.getDiffFields()).containsExactly("a");
        }

        @Test   
        @DisplayName("查询审计日志详情时，审计日志不存在抛出异常")
        void shouldThrow_whenAuditLogNotFound() {
            // Given：审计日志不存在
            given(auditLogQueryMapper.selectAuditLogById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.getAuditLogDetail(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.AUDIT_LOG_NOT_FOUND.getCode()))
                .hasMessage("审计日志不存在");
        }
    }

    @Nested
    @DisplayName("pageLoginLogs：分页查询登录日志")
    class PageLoginLogsTest {

        @Test   
        @DisplayName("分页查询登录日志时，返回登录日志 VO 分页结果")
        void shouldReturnLoginLogVOPage_whenRecordsExist() {
            // Given：一条登录日志记录
            SysLoginLog loginLog = LogTestFactory.loginLog(1L, 100L, "admin", 1,
                LocalDateTime.of(2026, 9, 28, 9, 0, 0), "192.168.1.10",
                "Mozilla/5.0", 1, null);
            Page<SysLoginLog> page = new Page<>(1, 20);
            page.setTotal(1);
            page.setRecords(List.of(loginLog));
            given(sysLoginLogMapper.selectPage(any(), any())).willReturn(page);
            var query = LogTestFactory.loginLogQueryDTO(1, 20, "admin", 1, 1, null, null);

            // When
            PageResult<LoginLogVO> result = service.pageLoginLogs(query);

            // Then：实体到 VO 的字段映射正确
            assertThat(result.total()).isEqualTo(1L);
            assertThat(result.pageNum()).isEqualTo(1L);
            assertThat(result.pageSize()).isEqualTo(20L);
            assertThat(result.pages()).isEqualTo(1L);
            List<LoginLogVO> vos = result.list();
            assertThat(vos).hasSize(1);
            LoginLogVO vo = vos.get(0);
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.userId()).isEqualTo(100L);
            assertThat(vo.username()).isEqualTo("admin");
            assertThat(vo.loginType()).isEqualTo(1);
            assertThat(vo.loginTime()).isEqualTo(LocalDateTime.of(2026, 9, 28, 9, 0, 0));
            assertThat(vo.loginIp()).isEqualTo("192.168.1.10");
            assertThat(vo.userAgent()).isEqualTo("Mozilla/5.0");
            assertThat(vo.resultStatus()).isEqualTo(1);
            assertThat(vo.failReason()).isNull();
        }

        @Test   
        @DisplayName("分页查询登录日志时，终点时间格式非法抛出异常")
        void shouldThrow_whenEndTimeInvalid() {
            // Given：终点时间格式非法
            var query = LogTestFactory.loginLogQueryDTO(1, 20, null, null, null, null, "2026-13-99");

            // When + Then
            assertThatThrownBy(() -> service.pageLoginLogs(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("时间参数格式非法，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
            verifyNoInteractions(sysLoginLogMapper);
        }
    }
}
