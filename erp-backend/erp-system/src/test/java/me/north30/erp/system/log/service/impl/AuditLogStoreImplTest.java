package me.north30.erp.system.log.service.impl;

import me.north30.erp.common.audit.AuditLogRecord;
import me.north30.erp.system.log.LogTestFactory;
import me.north30.erp.system.log.entity.SysAuditLog;
import me.north30.erp.system.log.mapper.SysAuditLogMapper;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * {@link AuditLogStoreImpl} 纯单元测试：字段映射、兜底值与防御性截断。
 */
@ExtendWith(MockitoExtension.class)
class AuditLogStoreImplTest {

    /** 超长截断标记（与被测类常量一致：... + (truncated) 共 14 字符） */
    private static final String TRUNCATED_MARKER = "...(truncated)";

    @Mock
    private SysAuditLogMapper sysAuditLogMapper;

    @InjectMocks
    private AuditLogStoreImpl service;

    @Captor
    private ArgumentCaptor<SysAuditLog> auditLogCaptor;

    @Nested
    @DisplayName("审计日志审计记录存储测试")
    class StoreTest {

        @Test   
        @DisplayName("存储成功审计记录，字段齐全")
        void shouldInsertMappedEntity_whenFullRecord() {
            // Given：字段齐全的成功审计记录
            AuditLogRecord record = LogTestFactory.fullRecord();

            // When
            service.store(record);

            // Then：17 个字段一一映射到 sys_audit_log 实体
            verify(sysAuditLogMapper).insert(auditLogCaptor.capture());
            SysAuditLog entity = auditLogCaptor.getValue();
            assertThat(entity.getModule()).isEqualTo("PURCHASE");
            assertThat(entity.getBizType()).isEqualTo("PURCHASE_ORDER");
            assertThat(entity.getBizCode()).isEqualTo("PO202609001");
            assertThat(entity.getOperateType()).isEqualTo("UPDATE");
            assertThat(entity.getOperateDesc()).isEqualTo("SysRoleServiceImpl.update");
            assertThat(entity.getOperateBy()).isEqualTo("admin");
            assertThat(entity.getOperateTime()).isEqualTo(LocalDateTime.of(2026, 9, 28, 10, 0, 0));
            assertThat(entity.getOperateIp()).isEqualTo("192.168.1.10");
            assertThat(entity.getRequestUri()).isEqualTo("/api/purchase/orders/1/approve");
            assertThat(entity.getRequestMethod()).isEqualTo("POST");
            assertThat(entity.getBeforeJson()).isEqualTo("{\"status\":0}");
            assertThat(entity.getAfterJson()).isEqualTo("{\"status\":1}");
            assertThat(entity.getResultStatus()).isEqualTo(1);
            assertThat(entity.getErrorCode()).isNull();
            assertThat(entity.getErrorMessage()).isNull();
            assertThat(entity.getCostTime()).isEqualTo(25L);
            assertThat(entity.getTraceId()).isEqualTo("trace-001");
        }

        @Test   
        @DisplayName("存储成功审计记录，字段不齐全")
        void shouldFallbackDefaults_whenRecordBlank() {
            // Given：module/操作人/操作时间/结果状态均为空（无登录上下文或系统操作）
            AuditLogRecord record = LogTestFactory.minimalRecord();

            // When
            service.store(record);

            // Then：模块兜底 UNKNOWN、操作人兜底 anonymous、结果兜底成功、时间兜底当前时间
            verify(sysAuditLogMapper).insert(auditLogCaptor.capture());
            SysAuditLog entity = auditLogCaptor.getValue();
            assertThat(entity.getModule()).isEqualTo("UNKNOWN");
            assertThat(entity.getOperateBy()).isEqualTo("anonymous");
            assertThat(entity.getOperateTime()).isNotNull();
            assertThat(entity.getResultStatus()).isEqualTo(1);
            assertThat(entity.getBizCode()).isNull();
            assertThat(entity.getBeforeJson()).isNull();
        }

        @Test   
        @DisplayName("存储成功审计记录，操作人为空白字符串时，操作人兜底为 anonymous")
        void shouldFallbackAnonymous_whenOperateByBlank() {
            // Given：操作人为空白字符串（非 null，同样触发兜底）
            AuditLogRecord record = LogTestFactory.record("SYSTEM", null, null, "UPDATE", null,
                "   ", LocalDateTime.of(2026, 9, 28, 10, 0, 0), null, null, null,
                null, null, 1, null, null, null, null);

            // When
            service.store(record);

            // Then
            verify(sysAuditLogMapper).insert(auditLogCaptor.capture());
            assertThat(auditLogCaptor.getValue().getOperateBy()).isEqualTo("anonymous");
        }

        @Test   
        @DisplayName("存储成功审计记录，字段超长时截断")
        void shouldTruncateOverlongFields_withMarker() {
            // Given：多个字段超过列长上限（bizCode>50、operateDesc>200、JSON>10000、errorMessage>500、traceId>64）
            AuditLogRecord record = LogTestFactory.recordWithOverflow(
                "B".repeat(60), "D".repeat(250), "J".repeat(10100), "{\"after\":1}",
                "E".repeat(600), "T".repeat(100));

            // When
            service.store(record);

            // Then：截断后总长等于列上限且追加截断标记
            verify(sysAuditLogMapper).insert(auditLogCaptor.capture());
            SysAuditLog entity = auditLogCaptor.getValue();
            assertThat(entity.getBizCode()).hasSize(50).endsWith(TRUNCATED_MARKER);
            assertThat(entity.getOperateDesc()).hasSize(200).endsWith(TRUNCATED_MARKER);
            assertThat(entity.getBeforeJson()).hasSize(10000).endsWith(TRUNCATED_MARKER);
            assertThat(entity.getAfterJson()).isEqualTo("{\"after\":1}");
            assertThat(entity.getErrorMessage()).hasSize(500).endsWith(TRUNCATED_MARKER);
            assertThat(entity.getTraceId()).hasSize(64).endsWith(TRUNCATED_MARKER);
        }

        @Test   
        @DisplayName("存储成功审计记录，字段长度恰好等于上限")
        void shouldKeepValue_whenLengthExactlyAtLimit() {
            // Given：字段长度恰好等于上限（边界：不截断、不追加标记）
            AuditLogRecord record = LogTestFactory.recordWithOverflow(
                "B".repeat(50), null, "J".repeat(10000), null, null, "T".repeat(64));

            // When
            service.store(record);

            // Then：原样保留
            verify(sysAuditLogMapper).insert(auditLogCaptor.capture());
            SysAuditLog entity = auditLogCaptor.getValue();
            assertThat(entity.getBizCode()).hasSize(50).doesNotEndWith(TRUNCATED_MARKER);
            assertThat(entity.getBeforeJson()).hasSize(10000).doesNotEndWith(TRUNCATED_MARKER);
            assertThat(entity.getTraceId()).hasSize(64).doesNotEndWith(TRUNCATED_MARKER);
        }
    }
}
