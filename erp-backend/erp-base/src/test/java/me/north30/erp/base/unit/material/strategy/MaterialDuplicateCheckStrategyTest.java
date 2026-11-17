package me.north30.erp.base.unit.material.strategy;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.entity.BaseMaterialCodeMap;
import me.north30.erp.base.material.mapper.BaseMaterialCodeMapMapper;
import me.north30.erp.base.material.mapper.BaseMaterialMapper;
import me.north30.erp.base.material.strategy.MaterialCodeMapStrategy;
import me.north30.erp.base.material.strategy.MaterialDuplicateCheckStrategy;
import me.north30.erp.base.unit.material.MaterialTestFactory;
import me.north30.erp.base.unit.support.MpTableInfoInit;
import me.north30.erp.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * {@link MaterialDuplicateCheckStrategy} 与 {@link MaterialCodeMapStrategy} 纯单元测试：
 * 编码唯一（11001）、四要素重复（11008）、旧码映射唯一（11015）、新物料存在（11002）。
 */
@ExtendWith(MockitoExtension.class)
class MaterialDuplicateCheckStrategyTest {

    @Mock
    private BaseMaterialMapper materialMapper;

    @Mock
    private BaseMaterialCodeMapMapper codeMapMapper;

    private MaterialDuplicateCheckStrategy duplicateCheckStrategy;

    private MaterialCodeMapStrategy codeMapStrategy;

    @BeforeAll
    static void initMpTableInfo() {
        MpTableInfoInit.init(BaseMaterial.class, BaseMaterialCodeMap.class);
    }

    @BeforeEach
    void setUp() {
        duplicateCheckStrategy = new MaterialDuplicateCheckStrategy(materialMapper);
        codeMapStrategy = new MaterialCodeMapStrategy(codeMapMapper, materialMapper);
    }

    @Nested
    @DisplayName("requireCodeAvailable：物料编码唯一")
    class RequireCodeAvailableTest {

        @Test
        @DisplayName("编码已存在，抛 11001")
        void shouldThrow_whenCodeExists() {
            given(materialMapper.selectCount(any())).willReturn(1L);

            assertThatThrownBy(() -> duplicateCheckStrategy.requireCodeAvailable("0103000001", null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_CODE_EXISTS.getCode()))
                .hasMessage("物料编码 0103000001 已存在");
        }

        @Test
        @DisplayName("编码未占用，通过")
        void shouldPass_whenCodeAvailable() {
            given(materialMapper.selectCount(any())).willReturn(0L);

            assertThatCode(() -> duplicateCheckStrategy.requireCodeAvailable("0103000009", null))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("requireFourElementsAvailable：四要素重复")
    class RequireFourElementsTest {

        @Test
        @DisplayName("四要素与既有物料一致，抛 11008（含既有物料编码提示）")
        void shouldThrow_whenDuplicated() {
            given(materialMapper.selectOne(any(LambdaQueryWrapper.class)))
                .willReturn(MaterialTestFactory.material(9L, "0103000002", "电阻", 0));

            assertThatThrownBy(() -> duplicateCheckStrategy
                .requireFourElementsAvailable("电阻", null, null, "个", null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_DUPLICATED.getCode()))
                .hasMessage("已存在相同物料（0103000002），请确认是否重复");
        }

        @Test
        @DisplayName("无同要素物料，通过")
        void shouldPass_whenNotDuplicated() {
            given(materialMapper.selectOne(any(LambdaQueryWrapper.class))).willReturn(null);

            assertThatCode(() -> duplicateCheckStrategy
                .requireFourElementsAvailable("新电阻", "0603", "YAGEO", "个", null))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("loadFourElementIndex：四要素索引预载")
    class LoadFourElementIndexTest {

        @Test
        @DisplayName("一次查询预载既有物料索引，键 = 名称|规格|品牌|单位")
        void shouldLoadIndex_whenRowsExist() {
            BaseMaterial existing = MaterialTestFactory.material(9L, "0103000002", "电阻", 0);
            existing.setSpec("0603");
            existing.setBrand("YAGEO");
            given(materialMapper.selectList(any(LambdaQueryWrapper.class))).willReturn(
                java.util.List.of(existing));

            Map<String, String> index = duplicateCheckStrategy.loadFourElementIndex();

            assertThat(index).containsEntry("电阻|0603|YAGEO|个", "0103000002");
        }

        @Test
        @DisplayName("fourElementKey：空值归一为空串")
        void shouldNormalizeNulls_whenBuildKey() {
            assertThat(MaterialDuplicateCheckStrategy.fourElementKey("电阻", null, " ", "个"))
                .isEqualTo("电阻|||个");
        }
    }

    @Nested
    @DisplayName("MaterialCodeMapStrategy：旧码映射守卫")
    class CodeMapStrategyTest {

        @Test
        @DisplayName("old_code + source_dept 已存在，抛 11015")
        void shouldThrow_whenMappingExists() {
            given(codeMapMapper.selectCount(any())).willReturn(1L);

            assertThatThrownBy(() -> codeMapStrategy.requireMappingAvailable("IC-001", "采购部", null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.CODE_MAP_EXISTS.getCode()))
                .hasMessage("旧物料编码 IC-001（采购部）映射已存在");
        }

        @Test
        @DisplayName("映射未占用，通过")
        void shouldPass_whenMappingAvailable() {
            given(codeMapMapper.selectCount(any())).willReturn(0L);

            assertThatCode(() -> codeMapStrategy.requireMappingAvailable("IC-001", "采购部", null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("新物料不存在，抛 11002")
        void shouldThrow_whenNewMaterialMissing() {
            given(materialMapper.selectById(99L)).willReturn(null);

            assertThatThrownBy(() -> codeMapStrategy.requireNewMaterialExists(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_NOT_FOUND.getCode()))
                .hasMessage("新物料不存在（id: 99）");
        }

        @Test
        @DisplayName("新物料存在，返回实体")
        void shouldReturn_whenNewMaterialExists() {
            BaseMaterial material = MaterialTestFactory.material(3L, "0103000001", "电阻", 0);
            given(materialMapper.selectById(3L)).willReturn(material);

            assertThat(codeMapStrategy.requireNewMaterialExists(3L)).isSameAs(material);
        }
    }
}
