package me.north30.erp.system.unit.config.service.impl;

import me.north30.erp.system.unit.config.ConfigTestFactory;
import me.north30.erp.system.unit.security.support.MpTableInfoInit;
import me.north30.erp.system.config.entity.SysConfig;
import me.north30.erp.system.config.mapper.SysConfigMapper;
import me.north30.erp.system.config.service.impl.SysConfigDomainServiceImpl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link SysConfigDomainServiceImpl} 纯单元测试：按类型读取参数、停用/缺失/非法值静默回落。
 */
@ExtendWith(MockitoExtension.class)
class SysConfigDomainServiceImplTest {

    @Mock
    private SysConfigMapper sysConfigMapper;

    @InjectMocks
    private SysConfigDomainServiceImpl service;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysConfig.class);
    }

    @Nested
    @DisplayName("longValue：读取数字型参数")
    class LongValueTest {

        @Test
        @DisplayName("参数存在且为合法数字，返回解析后的数字")
        void shouldReturnParsedNumber_whenValueValid() {
            given(sysConfigMapper.selectOne(any())).willReturn(
                ConfigTestFactory.config(1L, "inventory.stagnant.days", "呆滞库存判定天数", "180",
                    2, "INVENTORY", 1, 1));

            Long value = service.longValue("inventory.stagnant.days", 90L);

            assertThat(value).isEqualTo(180L);
        }

        @Test
        @DisplayName("参数值含首尾空白时先 trim 再解析")
        void shouldTrimValue_whenParsingNumber() {
            given(sysConfigMapper.selectOne(any())).willReturn(
                ConfigTestFactory.config(1L, "inventory.stagnant.days", "呆滞库存判定天数", " 180 ",
                    2, "INVENTORY", 1, 1));

            Long value = service.longValue("inventory.stagnant.days", 90L);

            assertThat(value).isEqualTo(180L);
        }

        @Test
        @DisplayName("参数不存在，返回回落值")
        void shouldReturnFallback_whenConfigMissing() {
            given(sysConfigMapper.selectOne(any())).willReturn(null);

            Long value = service.longValue("inventory.stagnant.days", 90L);

            assertThat(value).isEqualTo(90L);
        }

        @Test
        @DisplayName("参数已停用（status=0），查询不命中，返回回落值")
        void shouldReturnFallback_whenConfigDisabled() {
            // resolve 只查 status=1，停用参数视为不存在
            given(sysConfigMapper.selectOne(any())).willReturn(null);

            Long value = service.longValue("inventory.stagnant.days", 90L);

            assertThat(value).isEqualTo(90L);
        }

        @Test
        @DisplayName("参数值不是合法数字，返回回落值")
        void shouldReturnFallback_whenValueNotNumber() {
            given(sysConfigMapper.selectOne(any())).willReturn(
                ConfigTestFactory.config(1L, "inventory.stagnant.days", "呆滞库存判定天数", "abc",
                    2, "INVENTORY", 1, 1));

            Long value = service.longValue("inventory.stagnant.days", 90L);

            assertThat(value).isEqualTo(90L);
        }
    }

    @Nested
    @DisplayName("stringValue：读取字符串型参数")
    class StringValueTest {

        @Test
        @DisplayName("参数存在，返回 trim 后的字符串值")
        void shouldReturnTrimmedValue_whenConfigExists() {
            given(sysConfigMapper.selectOne(any())).willReturn(
                ConfigTestFactory.config(1L, "system.file.prefix", "文件名前缀", " erp- ",
                    1, "SYSTEM", 1, 1));

            String value = service.stringValue("system.file.prefix", "default-");

            assertThat(value).isEqualTo("erp-");
        }

        @Test
        @DisplayName("参数不存在，返回回落值")
        void shouldReturnFallback_whenConfigMissing() {
            given(sysConfigMapper.selectOne(any())).willReturn(null);

            String value = service.stringValue("system.file.prefix", "default-");

            assertThat(value).isEqualTo("default-");
        }
    }

    @Nested
    @DisplayName("booleanValue：读取布尔型参数")
    class BooleanValueTest {

        @Test
        @DisplayName("参数值为 true，返回 TRUE")
        void shouldReturnTrue_whenValueTrue() {
            given(sysConfigMapper.selectOne(any())).willReturn(
                ConfigTestFactory.config(1L, "inventory.expiry.outbound.block", "超期物料出库拦截开关", "true",
                    3, "INVENTORY", 1, 1));

            Boolean value = service.booleanValue("inventory.expiry.outbound.block", Boolean.FALSE);

            assertThat(value).isTrue();
        }

        @Test
        @DisplayName("参数值为 TRUE（大写），返回 TRUE（忽略大小写分支）")
        void shouldReturnTrue_whenValueTrueIgnoreCase() {
            given(sysConfigMapper.selectOne(any())).willReturn(
                ConfigTestFactory.config(1L, "inventory.expiry.outbound.block", "超期物料出库拦截开关", "TRUE",
                    3, "INVENTORY", 1, 1));

            Boolean value = service.booleanValue("inventory.expiry.outbound.block", Boolean.FALSE);

            assertThat(value).isTrue();
        }

        @Test
        @DisplayName("参数值为 1，返回 TRUE（数字布尔分支）")
        void shouldReturnTrue_whenValueOne() {
            given(sysConfigMapper.selectOne(any())).willReturn(
                ConfigTestFactory.config(1L, "inventory.expiry.outbound.block", "超期物料出库拦截开关", "1",
                    3, "INVENTORY", 1, 1));

            Boolean value = service.booleanValue("inventory.expiry.outbound.block", Boolean.FALSE);

            assertThat(value).isTrue();
        }

        @Test
        @DisplayName("参数值为 false，返回 FALSE")
        void shouldReturnFalse_whenValueFalse() {
            given(sysConfigMapper.selectOne(any())).willReturn(
                ConfigTestFactory.config(1L, "inventory.expiry.outbound.block", "超期物料出库拦截开关", "false",
                    3, "INVENTORY", 1, 1));

            Boolean value = service.booleanValue("inventory.expiry.outbound.block", Boolean.TRUE);

            assertThat(value).isFalse();
        }

        @Test
        @DisplayName("参数值为 0，返回 FALSE（数字布尔分支）")
        void shouldReturnFalse_whenValueZero() {
            given(sysConfigMapper.selectOne(any())).willReturn(
                ConfigTestFactory.config(1L, "inventory.expiry.outbound.block", "超期物料出库拦截开关", "0",
                    3, "INVENTORY", 1, 1));

            Boolean value = service.booleanValue("inventory.expiry.outbound.block", Boolean.TRUE);

            assertThat(value).isFalse();
        }

        @Test
        @DisplayName("参数值既非 true/false 也非 1/0，返回回落值")
        void shouldReturnFallback_whenValueInvalid() {
            given(sysConfigMapper.selectOne(any())).willReturn(
                ConfigTestFactory.config(1L, "inventory.expiry.outbound.block", "超期物料出库拦截开关", "yes",
                    3, "INVENTORY", 1, 1));

            Boolean value = service.booleanValue("inventory.expiry.outbound.block", Boolean.TRUE);

            assertThat(value).isTrue();
        }

        @Test
        @DisplayName("参数不存在，返回回落值")
        void shouldReturnFallback_whenConfigMissing() {
            given(sysConfigMapper.selectOne(any())).willReturn(null);

            Boolean value = service.booleanValue("inventory.expiry.outbound.block", Boolean.FALSE);

            assertThat(value).isFalse();
        }
    }

    @Nested
    @DisplayName("values：批量读取参数")
    class ValuesTest {

        @Test
        @DisplayName("多个 key 命中，返回 key 到 trim 后值的映射")
        void shouldReturnMap_whenKeysHit() {
            given(sysConfigMapper.selectList(any())).willReturn(List.of(
                ConfigTestFactory.config(1L, "system.page.default.size", "默认页大小", " 20 ",
                    2, "SYSTEM", 1, 1),
                ConfigTestFactory.config(2L, "system.page.max.size", "单页上限", "200",
                    2, "SYSTEM", 1, 1)));

            Map<String, String> values = service.values(List.of("system.page.default.size", "system.page.max.size"));

            assertThat(values).containsEntry("system.page.default.size", "20")
                .containsEntry("system.page.max.size", "200")
                .hasSize(2);
        }

        @Test
        @DisplayName("部分 key 已停用，不出现在结果中")
        void shouldOmitDisabledKeys() {
            given(sysConfigMapper.selectList(any())).willReturn(List.of(
                ConfigTestFactory.config(1L, "system.page.default.size", "默认页大小", "20",
                    2, "SYSTEM", 1, 1)));

            Map<String, String> values = service.values(List.of("system.page.default.size", "system.page.max.size"));

            assertThat(values).containsOnlyKeys("system.page.default.size");
        }

        @Test
        @DisplayName("空集合直接返回空 Map，不打库")
        void shouldReturnEmptyMap_whenKeysEmpty() {
            Map<String, String> values = service.values(List.of());

            assertThat(values).isEmpty();
            verify(sysConfigMapper, never()).selectList(any());
        }
    }
}
