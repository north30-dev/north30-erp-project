package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.dict.DictTestFactory;
import me.north30.erp.system.dict.entity.SysDictType;
import me.north30.erp.system.dict.mapper.SysDictTypeMapper;
import me.north30.erp.system.support.MpTableInfoInit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * {@link SysDictTypeServiceImpl} 纯单元测试：类型存在性守卫
 * （管理端 8 个用例的 CRUD 编排见 {@link DictManagementServiceImplTest}）。
 */
@ExtendWith(MockitoExtension.class)
class SysDictTypeServiceImplTest {

    @Mock
    private SysDictTypeMapper sysDictTypeMapper;

    @InjectMocks
    private SysDictTypeServiceImpl service;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysDictType.class);
    }

    @Test
    @DisplayName("类型编码存在，返回实体")
    void shouldReturnEntity_whenTypeExists() {
        // Given
        SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
        given(sysDictTypeMapper.selectOne(any(LambdaQueryWrapper.class))).willReturn(type);

        // When
        SysDictType result = service.requireByDictType("settlement_method");

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDictType()).isEqualTo("settlement_method");
    }

    @Test
    @DisplayName("类型编码不存在，抛出 18028 业务异常")
    void shouldThrow_whenTypeNotFound() {
        // Given
        given(sysDictTypeMapper.selectOne(any(LambdaQueryWrapper.class))).willReturn(null);

        // When + Then
        assertThatThrownBy(() -> service.requireByDictType("not_exist"))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.DICT_TYPE_NOT_FOUND.getCode()))
            .hasMessage("字典类型 not_exist 不存在");
    }
}
