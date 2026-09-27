package me.north30.erp.system.core.service.impl;

import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditLogAspect;
import me.north30.erp.common.audit.AuditLogRecord;
import me.north30.erp.common.audit.AuditLogStore;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.SystemErrorCode;
import me.north30.erp.common.security.CurrentUserProvider;
import me.north30.erp.system.core.entity.SysAuditLog;
import me.north30.erp.system.core.mapper.SysAuditLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 审计日志落库单元测试：AuditLogStoreImpl 字段映射/兜底/截断，
 * 以及 AuditLogAspect 落库失败不影响主流程、无 Store Bean 降级、异常记录并重抛。
 */
@ExtendWith(MockitoExtension.class)
class AuditLogStoreImplTest {

    /** 切面测试用的注解载体（注解实例须从带注解的方法上反射获取） */
    static class DummyAuditedService {

        @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.CREATE)
        public Object doWork() {
            return "ok";
        }
    }

    /** 序列化即抛异常的毒丸对象（getter 抛异常触发 Jackson 序列化失败） */
    static class PoisonBean {

        public String getName() {
            throw new IllegalStateException("boom");
        }
    }

    @Mock
    private SysAuditLogMapper sysAuditLogMapper;

    private AuditLogStoreImpl auditLogStore;

    @BeforeEach
    void setUp() {
        auditLogStore = new AuditLogStoreImpl(sysAuditLogMapper);
    }

    private AuditLogRecord fullRecord() {
        return new AuditLogRecord("SYSTEM", null, "SO001", "CREATE", "TestService.create()",
            "admin", LocalDateTime.of(2026, 9, 27, 10, 0, 0), "192.168.1.10",
            "/api/system/users", "POST", "{\"username\":\"zhangsan\"}", "{\"code\":200}",
            1, null, null, 15L, "trace-abc");
    }

    // ---------------------------------------------------------------- AuditLogStoreImpl

    @Test
    @DisplayName("落库：字段映射正确")
    void store_mapsAllFieldsCorrectly() {
        AuditLogRecord record = fullRecord();

        auditLogStore.store(record);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(sysAuditLogMapper).insert(captor.capture());
        SysAuditLog entity = captor.getValue();
        assertEquals("SYSTEM", entity.getModule());
        assertEquals("SO001", entity.getBizCode());
        assertEquals("CREATE", entity.getOperateType());
        assertEquals("TestService.create()", entity.getOperateDesc());
        assertEquals("admin", entity.getOperateBy());
        assertEquals(LocalDateTime.of(2026, 9, 27, 10, 0, 0), entity.getOperateTime());
        assertEquals("192.168.1.10", entity.getOperateIp());
        assertEquals("/api/system/users", entity.getRequestUri());
        assertEquals("POST", entity.getRequestMethod());
        assertEquals("{\"username\":\"zhangsan\"}", entity.getBeforeJson());
        assertEquals("{\"code\":200}", entity.getAfterJson());
        assertEquals(1, entity.getResultStatus());
        assertNull(entity.getErrorCode());
        assertNull(entity.getErrorMessage());
        assertEquals(15L, entity.getCostTime());
        assertEquals("trace-abc", entity.getTraceId());
    }

    @Test
    @DisplayName("落库：操作人为空兜底 anonymous，缺失字段兜底默认值")
    void store_blankOperator_fallsBackToAnonymous() {
        AuditLogRecord record = new AuditLogRecord(null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null);

        auditLogStore.store(record);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(sysAuditLogMapper).insert(captor.capture());
        SysAuditLog entity = captor.getValue();
        assertEquals("UNKNOWN", entity.getModule());
        assertEquals("anonymous", entity.getOperateBy());
        assertNotNull(entity.getOperateTime());
        assertEquals(1, entity.getResultStatus());
    }

    @Test
    @DisplayName("落库：超长字段按列长截断并追加截断标记")
    void store_longFields_truncated() {
        AuditLogRecord record = new AuditLogRecord("SYSTEM", null, null, null, null,
            "admin", null, null, null, null, null, null, 1,
            null, "错".repeat(600), 0L, null);

        auditLogStore.store(record);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(sysAuditLogMapper).insert(captor.capture());
        assertEquals(500, captor.getValue().getErrorMessage().length());
        assertTrue(captor.getValue().getErrorMessage().endsWith("...(truncated)"));
    }

    // ---------------------------------------------------------------- AuditLogAspect

    @Test
    @DisplayName("切面：成功时构造记录并落库，主流程返回值不变")
    void aspect_persistsRecordOnSuccess() throws Throwable {
        AuditLogStore store = mock(AuditLogStore.class);
        AuditLogAspect aspect = newAspect(store);
        ProceedingJoinPoint joinPoint = proceedingJoinPoint(List.of("arg-1"));
        AuditLog auditLog = resolveAuditLogAnnotation();

        Object result = aspect.around(joinPoint, auditLog);

        assertEquals("ok", result);
        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(store).store(captor.capture());
        AuditLogRecord record = captor.getValue();
        assertEquals("SYSTEM", record.module());
        assertEquals("CREATE", record.operateType());
        assertEquals("Test.doWork()", record.operateDesc());
        assertEquals(1, record.resultStatus());
        // 入参 JSON 序列化进 beforeJson，返回值序列化进 afterJson
        assertTrue(record.beforeJson().contains("arg-1"));
        assertTrue(record.afterJson().contains("ok"));
    }

    @Test
    @DisplayName("切面：落库失败仅告警，不影响主流程")
    void aspect_storeFailure_doesNotAffectMainFlow() throws Throwable {
        AuditLogStore store = mock(AuditLogStore.class);
        doThrow(new RuntimeException("db down")).when(store).store(any());
        AuditLogAspect aspect = newAspect(store);
        ProceedingJoinPoint joinPoint = proceedingJoinPoint(List.of("arg-1"));
        AuditLog auditLog = resolveAuditLogAnnotation();

        Object result = aspect.around(joinPoint, auditLog);

        assertEquals("ok", result);
        verify(store).store(any(AuditLogRecord.class));
    }

    @Test
    @DisplayName("切面：容器中无 AuditLogStore Bean 时保持 SLF4J 行为，不落库")
    void aspect_noStoreBean_degradesToSlf4j() throws Throwable {
        AuditLogStore store = mock(AuditLogStore.class);
        AuditLogAspect aspect = newAspect(null);
        ProceedingJoinPoint joinPoint = proceedingJoinPoint(List.of("arg-1"));
        AuditLog auditLog = resolveAuditLogAnnotation();

        Object result = aspect.around(joinPoint, auditLog);

        assertEquals("ok", result);
        verifyNoInteractions(store);
    }

    @Test
    @DisplayName("切面：业务异常记录错误码后原样重抛")
    void aspect_businessException_recordedAndRethrown() throws Throwable {
        AuditLogStore store = mock(AuditLogStore.class);
        AuditLogAspect aspect = newAspect(store);
        ProceedingJoinPoint joinPoint = proceedingJoinPoint(List.of("arg-1"));
        when(joinPoint.proceed()).thenThrow(
            new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 2 不存在"));
        AuditLog auditLog = resolveAuditLogAnnotation();

        BusinessException ex = assertThrows(BusinessException.class, () -> aspect.around(joinPoint, auditLog));

        assertEquals("用户 2 不存在", ex.getMessage());
        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(store).store(captor.capture());
        AuditLogRecord record = captor.getValue();
        assertEquals(0, record.resultStatus());
        assertEquals(18005, record.errorCode());
        assertEquals("用户 2 不存在", record.errorMessage());
        assertNull(record.afterJson());
    }

    @Test
    @DisplayName("切面：入参不可序列化时跳过该字段，主流程不受影响")
    void aspect_unserializableArg_skippedGracefully() throws Throwable {
        AuditLogStore store = mock(AuditLogStore.class);
        AuditLogAspect aspect = newAspect(store);
        ProceedingJoinPoint joinPoint = proceedingJoinPoint(new PoisonBean());
        AuditLog auditLog = resolveAuditLogAnnotation();

        Object result = aspect.around(joinPoint, auditLog);

        assertEquals("ok", result);
        ArgumentCaptor<AuditLogRecord> captor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(store).store(captor.capture());
        AuditLogRecord record = captor.getValue();
        // 序列化失败 → beforeJson 为 null，但记录仍落库
        assertNull(record.beforeJson());
        assertEquals(1, record.resultStatus());
    }

    /**
     * 构造被测切面：ObjectProvider 以 Mock 模拟容器行为（store 为 null 表示容器中无实现 Bean）。
     */
    @SuppressWarnings("unchecked")
    private AuditLogAspect newAspect(AuditLogStore store) {
        ObjectProvider<AuditLogStore> storeProvider = mock(ObjectProvider.class);
        when(storeProvider.getIfAvailable()).thenReturn(store);
        ObjectProvider<CurrentUserProvider> userProvider = mock(ObjectProvider.class);
        when(userProvider.getIfAvailable()).thenReturn(null);
        return new AuditLogAspect(storeProvider, userProvider, JsonMapper.builder().build());
    }

    /**
     * 构造 ProceedingJoinPoint Mock：proceed 返回 "ok"，入参为给定对象数组。
     * getArgs 仅在落库分支（buildRecord）中被调用，无 Store Bean 的用例不会触达，故用 lenient。
     */
    private ProceedingJoinPoint proceedingJoinPoint(Object... args) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("Test.doWork()");
        when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn("ok");
        return joinPoint;
    }

    private AuditLog resolveAuditLogAnnotation() {
        try {
            return DummyAuditedService.class.getDeclaredMethod("doWork").getAnnotation(AuditLog.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
