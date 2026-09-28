package me.north30.erp.system.config.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.common.vo.MutationVO;
import me.north30.erp.system.config.ConfigTestFactory;
import me.north30.erp.system.config.entity.SysConfig;
import me.north30.erp.system.config.mapper.SysConfigMapper;
import me.north30.erp.system.config.vo.ConfigUpdateVO;
import me.north30.erp.system.config.vo.ConfigVO;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link SysConfigServiceImpl} 纯单元测试：分页归一化、参数值按类型校验、键唯一、乐观锁与内置参数删除约束。
 */
@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {

    @Mock
    private SysConfigMapper sysConfigMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SysConfigServiceImpl service;

    @Captor
    private ArgumentCaptor<Page<SysConfig>> pageCaptor;

    @Captor
    private ArgumentCaptor<SysConfig> configCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysConfig.class);
    }

    @Nested
    @DisplayName("page：分页查询系统参数")
    class PageTest {

        @Test
        @DisplayName("分页查询系统参数，返回参数列表")
        void shouldReturnVOPage_whenRecordsExist() {
            // Given：两条参数记录
            SysConfig c1 = ConfigTestFactory.config(1L, "inventory.stagnant.days", "滞销天数阈值", "90",
                2, "INVENTORY", 1, 1);
            SysConfig c2 = ConfigTestFactory.config(2L, "sales.discount.max", "最大折扣", "0.8",
                2, "SALES", 0, 0);
            Page<SysConfig> page = new Page<>(1, 20);
            page.setTotal(2);
            page.setRecords(List.of(c1, c2));
            given(sysConfigMapper.selectPage(any(), any())).willReturn(page);

            // When
            PageResult<ConfigVO> result = service.page(ConfigTestFactory.queryDTO(null, null, null, null, 1, 20));

            // Then：分页字段与 VO 映射正确
            assertThat(result.total()).isEqualTo(2L);
            assertThat(result.pageNum()).isEqualTo(1L);
            assertThat(result.pageSize()).isEqualTo(20L);
            assertThat(result.pages()).isEqualTo(1L);
            List<ConfigVO> vos = result.list();
            assertThat(vos).hasSize(2);
            assertThat(vos.get(0).id()).isEqualTo(1L);
            assertThat(vos.get(0).configKey()).isEqualTo("inventory.stagnant.days");
            assertThat(vos.get(0).configName()).isEqualTo("滞销天数阈值");
            assertThat(vos.get(0).configValue()).isEqualTo("90");
            assertThat(vos.get(0).valueType()).isEqualTo(2);
            assertThat(vos.get(0).configGroup()).isEqualTo("INVENTORY");
            assertThat(vos.get(0).isSystem()).isEqualTo(1);
            assertThat(vos.get(0).status()).isEqualTo(1);
            assertThat(vos.get(1).configKey()).isEqualTo("sales.discount.max");
        }

        @Test
        @DisplayName("分页查询系统参数，每页条数不能超过 200")
        void shouldThrow_whenPageSizeExceedsMax() {
            // Given：每页条数 201 超过上限 200
            var query = ConfigTestFactory.queryDTO(null, null, null, null, 1, 201);

            // When + Then
            assertThatThrownBy(() -> service.page(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("pageSize 不能超过 200");
            verifyNoInteractions(sysConfigMapper);
        }

        @Test
        @DisplayName("分页查询系统参数，页码小于 1 时归位为默认页码")
        void shouldNormalizePageNum_whenNonPositive() {
            // Given：页码小于 1 时归位为默认页码
            Page<SysConfig> page = new Page<>(1, 20);
            page.setTotal(0);
            page.setRecords(List.of());
            given(sysConfigMapper.selectPage(any(), any())).willReturn(page);

            // When
            service.page(ConfigTestFactory.queryDTO(null, null, null, null, 0, 20));

            // Then
            verify(sysConfigMapper).selectPage(pageCaptor.capture(), any());
            assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
            assertThat(pageCaptor.getValue().getSize()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("create：创建系统参数")
    class CreateTest {

        @Test
        @DisplayName("创建系统参数，值类型 1-字符串不做格式校验，isSystem/status 缺省触发默认值")
        void shouldInsertWithDefaults_whenValueTypeString() {
            // Given：值类型 1-字符串不做格式校验，isSystem/status 缺省触发默认值
            var dto = ConfigTestFactory.createDTO("inventory.stagnant.days", "滞销天数阈值", "90",
                1, "INVENTORY", null, null, "库存域参数");
            given(sysConfigMapper.selectCount(any())).willReturn(0L);
            given(sysConfigMapper.insert(any(SysConfig.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysConfig.class).setId(7L);
                return 1;
            });

            // When
            MutationVO vo = service.create(dto);

            // Then
            assertThat(vo.id()).isEqualTo(7L);
            assertThat(vo.updateTime()).isNull();
            assertThat(vo.isDeleted()).isNull();
            verify(sysConfigMapper).insert(configCaptor.capture());
            assertThat(configCaptor.getValue().getConfigKey()).isEqualTo("inventory.stagnant.days");
            assertThat(configCaptor.getValue().getConfigName()).isEqualTo("滞销天数阈值");
            assertThat(configCaptor.getValue().getConfigValue()).isEqualTo("90");
            assertThat(configCaptor.getValue().getValueType()).isEqualTo(1);
            assertThat(configCaptor.getValue().getConfigGroup()).isEqualTo("INVENTORY");
            assertThat(configCaptor.getValue().getIsSystem()).isZero();
            assertThat(configCaptor.getValue().getStatus()).isEqualTo(1);
            assertThat(configCaptor.getValue().getRemark()).isEqualTo("库存域参数");
        }

        @Test
        @DisplayName("创建系统参数，值类型 3-布尔，TRUE 大写合法（equalsIgnoreCase 分支）")
        void shouldAcceptBooleanValueIgnoreCase_whenValueTypeBoolean() {
            // Given：值类型 3-布尔，TRUE 大写合法（equalsIgnoreCase 分支）
            var dto = ConfigTestFactory.createDTO("sales.enabled", "销售启用", "TRUE", 3, "SALES", 0, 1, null);
            given(sysConfigMapper.selectCount(any())).willReturn(0L);
            given(sysConfigMapper.insert(any(SysConfig.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysConfig.class).setId(8L);
                return 1;
            });

            // When
            MutationVO vo = service.create(dto);

            // Then
            assertThat(vo.id()).isEqualTo(8L);
            verify(sysConfigMapper).insert(configCaptor.capture());
            assertThat(configCaptor.getValue().getConfigValue()).isEqualTo("TRUE");
        }

        @Test
        @DisplayName("创建系统参数，值类型 4-JSON 且内容可解析，isSystem/status 缺省触发默认值")
        void shouldAcceptJsonValue_whenValueTypeJson() {
            // Given：值类型 4-JSON 且内容可解析
            var dto = ConfigTestFactory.createDTO("purchase.rule", "采购规则", "{\"threshold\":90}",
                4, "PURCHASE", 0, 1, null);
            given(objectMapper.readTree("{\"threshold\":90}")).willReturn(null);
            given(sysConfigMapper.selectCount(any())).willReturn(0L);
            given(sysConfigMapper.insert(any(SysConfig.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysConfig.class).setId(9L);
                return 1;
            });

            // When
            MutationVO vo = service.create(dto);

            // Then
            assertThat(vo.id()).isEqualTo(9L);
            verify(sysConfigMapper).insert(configCaptor.capture());
            assertThat(configCaptor.getValue().getValueType()).isEqualTo(4);
        }

        @Test
        @DisplayName("创建系统参数，键值重复时抛出异常")
        void shouldThrow_whenConfigKeyExists() {
            // Given：参数键已存在（计数大于 0）
            var dto = ConfigTestFactory.createDTO("inventory.stagnant.days", "滞销天数阈值", "90",
                2, "INVENTORY", 0, 1, null);
            given(sysConfigMapper.selectCount(any())).willReturn(1L);

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.DUPLICATE_KEY.getCode()))
                .hasMessage("参数键 inventory.stagnant.days 已存在，请检查后重试");
            verify(sysConfigMapper, never()).insert(any(SysConfig.class));
        }

        @Test
        @DisplayName("创建系统参数，值类型 2-数字，值不可解析为数字时抛出异常")
        void shouldThrow_whenNumericValueInvalid() {
            // Given：值类型 2-数字，值不可解析为数字
            var dto = ConfigTestFactory.createDTO("inventory.stagnant.days", "滞销天数阈值", "abc",
                2, "INVENTORY", 0, 1, null);

            // When + Then：值校验先于唯一性查询，Mapper 与 ObjectMapper 均不应被调用
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.CONFIG_VALUE_TYPE_MISMATCH.getCode()))
                .hasMessage("参数值类型错误，期望类型 数字");
            verifyNoInteractions(sysConfigMapper, objectMapper);
        }

        @Test
        @DisplayName("创建系统参数，值类型 3-布尔，值既非 true 也非 false 时抛出异常")
        void shouldThrow_whenBooleanValueInvalid() {
            // Given：值类型 3-布尔，值既非 true 也非 false
            var dto = ConfigTestFactory.createDTO("sales.enabled", "销售启用", "yes", 3, "SALES", 0, 1, null);

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.CONFIG_VALUE_TYPE_MISMATCH.getCode()))
                .hasMessage("参数值类型错误，期望类型 布尔");
            verifyNoInteractions(sysConfigMapper);
        }

        @Test
        @DisplayName("创建系统参数，值类型 4-JSON 解析抛出 JacksonException 时抛出异常")
        void shouldThrow_whenJsonValueInvalid() {
            // Given：值类型 4-JSON，解析抛出 JacksonException
            var dto = ConfigTestFactory.createDTO("purchase.rule", "采购规则", "not-json", 4, "PURCHASE", 0, 1, null);
            given(objectMapper.readTree("not-json")).willThrow(new JacksonException("boom") {
            });

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.CONFIG_VALUE_TYPE_MISMATCH.getCode()))
                .hasMessage("参数值类型错误，期望类型 JSON");
            verify(sysConfigMapper, never()).insert(any(SysConfig.class));
        }

        @Test
        @DisplayName("创建系统参数，值类型超出 1-4 范围时抛出异常")
        void shouldThrow_whenValueTypeIllegal() {
            // Given：值类型超出 1-4 范围
            var dto = ConfigTestFactory.createDTO("some.key", "某参数", "1", 99, "SYSTEM", 0, 1, null);

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("参数值类型 99 非法");
            verifyNoInteractions(sysConfigMapper, objectMapper);
        }
    }

    @Nested
    @DisplayName("更新系统参数")
    class UpdateTest {

        @Test
        @DisplayName("更新系统参数，参数存在，DTO 全字段提供")
        void shouldUpdateProvidedFields_whenConfigExists() {
            // Given：参数存在，DTO 全字段提供
            SysConfig config = ConfigTestFactory.config(1L, "inventory.stagnant.days", "旧名称", "90",
                2, "INVENTORY", 0, 1);
            config.setUpdateTime(LocalDateTime.of(2026, 9, 28, 10, 0, 0));
            given(sysConfigMapper.selectById(1L)).willReturn(config);
            given(sysConfigMapper.updateById(any(SysConfig.class))).willReturn(1);
            var dto = ConfigTestFactory.updateDTO("120", "新名称", 0, "阈值调整", 5);

            // When
            ConfigUpdateVO vo = service.update(1L, dto);

            // Then：返回生效提示与格式化更新时间，实体按 DTO 更新
            assertThat(vo.effectNote()).isEqualTo("参数将在 1 分钟内生效");
            assertThat(vo.updateTime()).isEqualTo("2026-09-28 10:00:00");
            verify(sysConfigMapper).updateById(configCaptor.capture());
            assertThat(configCaptor.getValue().getConfigValue()).isEqualTo("120");
            assertThat(configCaptor.getValue().getConfigName()).isEqualTo("新名称");
            assertThat(configCaptor.getValue().getStatus()).isZero();
            assertThat(configCaptor.getValue().getRemark()).isEqualTo("阈值调整");
            assertThat(configCaptor.getValue().getVersion()).isEqualTo(5);
        }

        @Test
        @DisplayName("更新系统参数，参数存在，DTO 仅提供 configValue 与 version，其余为 null 表示不修改")
        void shouldKeepOriginalFields_whenDtoFieldsNull() {
            // Given：DTO 仅提供 configValue 与 version，其余为 null 表示不修改
            SysConfig config = ConfigTestFactory.config(1L, "inventory.stagnant.days", "旧名称", "90",
                2, "INVENTORY", 0, 1);
            config.setRemark("原备注");
            given(sysConfigMapper.selectById(1L)).willReturn(config);
            given(sysConfigMapper.updateById(any(SysConfig.class))).willReturn(1);
            var dto = ConfigTestFactory.updateDTO("200", null, null, null, 3);

            // When
            service.update(1L, dto);

            // Then：未提供字段保持原值
            verify(sysConfigMapper).updateById(configCaptor.capture());
            assertThat(configCaptor.getValue().getConfigValue()).isEqualTo("200");
            assertThat(configCaptor.getValue().getConfigName()).isEqualTo("旧名称");
            assertThat(configCaptor.getValue().getStatus()).isEqualTo(1);
            assertThat(configCaptor.getValue().getRemark()).isEqualTo("原备注");
            assertThat(configCaptor.getValue().getVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("更新系统参数，参数不存在，抛出异常")
        void shouldThrow_whenConfigNotFound() {
            // Given：参数不存在
            given(sysConfigMapper.selectById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, ConfigTestFactory.updateDTO("120", null, null, null, 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.CONFIG_NOT_FOUND.getCode()))
                .hasMessage("系统参数 1 不存在");
            verify(sysConfigMapper, never()).updateById(any(SysConfig.class));
        }

        @Test
        @DisplayName("更新系统参数，参数存在，值类型 3-布尔，新既非 true 也非 false 时抛出异常")
        void shouldThrow_whenValueMismatchOnUpdate() {
            // Given：按实体已有值类型（3-布尔）校验新值，"9" 非布尔
            SysConfig config = ConfigTestFactory.config(1L, "sales.enabled", "销售启用", "true",
                3, "SALES", 0, 1);
            given(sysConfigMapper.selectById(1L)).willReturn(config);
            var dto = ConfigTestFactory.updateDTO("9", null, null, null, 1);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.CONFIG_VALUE_TYPE_MISMATCH.getCode()))
                .hasMessage("参数值类型错误，期望类型 布尔");
            verify(sysConfigMapper, never()).updateById(any(SysConfig.class));
        }

        @Test
        @DisplayName("更新系统参数，参数存在，version 冲突，抛出异常")
        void shouldThrow_whenOptimisticLockConflict() {
            // Given：更新影响行数为 0（version 冲突）
            SysConfig config = ConfigTestFactory.config(1L, "inventory.stagnant.days", "滞销天数阈值", "90",
                2, "INVENTORY", 0, 1);
            given(sysConfigMapper.selectById(1L)).willReturn(config);
            given(sysConfigMapper.updateById(any(SysConfig.class))).willReturn(0);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, ConfigTestFactory.updateDTO("120", null, null, null, 9)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()))
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }

    @Nested
    @DisplayName("删除系统参数")
    class DeleteTest {

        @Test
        @DisplayName("删除系统参数，参数存在，非内置参数，成功删除")
        void shouldDelete_whenNotBuiltin() {
            // Given：非内置参数（isSystem=0）
            SysConfig config = ConfigTestFactory.config(1L, "inventory.stagnant.days", "滞销天数阈值", "90",
                2, "INVENTORY", 0, 1);
            given(sysConfigMapper.selectById(1L)).willReturn(config);
            given(sysConfigMapper.deleteById(1L)).willReturn(1);

            // When
            MutationVO vo = service.delete(1L);

            // Then
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.updateTime()).isNull();
            assertThat(vo.isDeleted()).isEqualTo(1);
            verify(sysConfigMapper).deleteById(1L);
        }

        @Test
        @DisplayName("删除系统参数，参数存在，内置参数，抛出异常")
        void shouldThrow_whenBuiltinConfig() {
            // Given：内置参数（isSystem=1）不可删除
            SysConfig config = ConfigTestFactory.config(1L, "inventory.stagnant.days", "滞销天数阈值", "90",
                2, "INVENTORY", 1, 1);
            given(sysConfigMapper.selectById(1L)).willReturn(config);

            // When + Then
            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.BUILTIN_CONFIG_UNDELETABLE.getCode()))
                .hasMessage("内置系统参数 inventory.stagnant.days 不可删除");
            verify(sysConfigMapper, never()).deleteById(anyLong());
        }
        
        @Test
        @DisplayName("删除系统参数，参数不存在，抛出异常")
        void shouldThrow_whenConfigNotFound() {
            // Given：参数不存在
            given(sysConfigMapper.selectById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.CONFIG_NOT_FOUND.getCode()))
                .hasMessage("系统参数 1 不存在");
            verify(sysConfigMapper, never()).deleteById(anyLong());
        }
    }
}
