package me.north30.erp.system.log.service.impl;

import me.north30.erp.system.log.LogTestFactory;
import me.north30.erp.system.log.entity.SysLoginLog;
import me.north30.erp.system.log.mapper.SysLoginLogMapper;

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
 * {@link SysLoginLogServiceImpl} 纯单元测试：登录日志落库透传。
 */
@ExtendWith(MockitoExtension.class)
class SysLoginLogServiceImplTest {

    @Mock
    private SysLoginLogMapper sysLoginLogMapper;

    @InjectMocks
    private SysLoginLogServiceImpl service;

    @Captor
    private ArgumentCaptor<SysLoginLog> loginLogCaptor;

    @Nested
    @DisplayName("登录日志记录测试")
    class RecordTest {

        @Test   
        @DisplayName("记录登录成功日志")
        void shouldInsertLoginLog_whenRecorded() {
            // Given：一条登录成功日志
            SysLoginLog loginLog = LogTestFactory.loginLog(1L, 100L, "admin", 1,
                LocalDateTime.of(2026, 9, 28, 9, 0, 0), "192.168.1.10",
                "Mozilla/5.0", 1, null);

            // When
            service.record(loginLog);

            // Then：原样透传给 Mapper 插入
            verify(sysLoginLogMapper).insert(loginLogCaptor.capture());
            assertThat(loginLogCaptor.getValue()).isSameAs(loginLog);
            assertThat(loginLogCaptor.getValue().getUsername()).isEqualTo("admin");
            assertThat(loginLogCaptor.getValue().getLoginType()).isEqualTo(1);
            assertThat(loginLogCaptor.getValue().getResultStatus()).isEqualTo(1);
        }
        
        @Test   
        @DisplayName("记录登录失败日志（用户不存在时 userId 为空并携带失败原因）")
        void shouldInsertLoginLog_whenLoginFailed() {
            // Given：一条登录失败日志（用户不存在时 userId 为空并携带失败原因）
            SysLoginLog loginLog = LogTestFactory.loginLog(null, null, "ghost", 4,
                LocalDateTime.of(2026, 9, 28, 9, 0, 0), "192.168.1.10",
                "Mozilla/5.0", 0, "账号或密码错误");

            // When
            service.record(loginLog);

            // Then
            verify(sysLoginLogMapper).insert(loginLogCaptor.capture());
            assertThat(loginLogCaptor.getValue().getUserId()).isNull();
            assertThat(loginLogCaptor.getValue().getResultStatus()).isZero();
            assertThat(loginLogCaptor.getValue().getFailReason()).isEqualTo("账号或密码错误");
        }
    }
}
