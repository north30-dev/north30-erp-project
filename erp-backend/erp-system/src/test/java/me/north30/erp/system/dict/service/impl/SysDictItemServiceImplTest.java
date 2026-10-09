package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import me.north30.erp.system.dict.mapper.SysDictItemMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link SysDictItemServiceImpl} 纯单元测试：字典项分组统计守卫
 * （管理端 8 个用例的 CRUD 编排见 {@link DictManagementServiceImplTest}）。
 */
@ExtendWith(MockitoExtension.class)
class SysDictItemServiceImplTest {

    @Mock
    private SysDictItemMapper sysDictItemMapper;

    @InjectMocks
    private SysDictItemServiceImpl service;

    @Test
    @DisplayName("空类型列表，直接返回空映射且不查库")
    void shouldReturnEmptyMap_whenTypeListEmpty() {
        // When
        Map<String, Long> counts = service.countByTypes(List.of());

        // Then
        assertThat(counts).isEmpty();
        verifyNoInteractions(sysDictItemMapper);
    }

    @Test
    @DisplayName("分组统计结果按 dict_type 映射为计数")
    void shouldMapCounts_whenRowsReturned() {
        // Given：一条 group by 查询返回三行，null dict_type 行被跳过（Map.of 不支持 null 值，用 HashMap 构造）
        Map<String, Object> nullRow = new HashMap<>();
        nullRow.put("dict_type", null);
        nullRow.put("cnt", 1);
        given(sysDictItemMapper.selectMaps(any(QueryWrapper.class))).willReturn(List.of(
            Map.of("dict_type", "settlement_method", "cnt", 3),
            Map.of("dict_type", "yes_no", "cnt", 5L),
            nullRow));

        // When
        Map<String, Long> counts = service.countByTypes(List.of("settlement_method", "yes_no"));

        // Then：Number 类型统一收敛为 long，null dict_type 跳过
        assertThat(counts).containsOnly(
            Map.entry("settlement_method", 3L),
            Map.entry("yes_no", 5L));
    }
}
