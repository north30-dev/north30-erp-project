package me.north30.erp.base.unit.material.service.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.dto.MaterialQueryDTO;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.entity.BaseMaterialCodeMap;
import me.north30.erp.base.material.mapper.BaseMaterialCodeMapMapper;
import me.north30.erp.base.material.mapper.BaseMaterialMapper;
import me.north30.erp.base.material.service.impl.MaterialDomainServiceImpl;
import me.north30.erp.base.material.strategy.MaterialCodeMapStrategy;
import me.north30.erp.base.material.strategy.MaterialDuplicateCheckStrategy;
import me.north30.erp.base.material.strategy.MaterialQueryStrategy;
import me.north30.erp.base.material.strategy.MaterialStatusStrategy;
import me.north30.erp.base.unit.material.MaterialTestFactory;
import me.north30.erp.base.unit.support.MpTableInfoInit;
import me.north30.erp.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link MaterialDomainServiceImpl} 纯单元测试：物料存在性、分类计数、状态机流转编排
 * （patch 持久化携带乐观锁版本）、旧码映射守卫委派。
 */
@ExtendWith(MockitoExtension.class)
class MaterialDomainServiceImplTest {

    @Mock
    private BaseMaterialMapper baseMaterialMapper;

    @Mock
    private BaseMaterialCodeMapMapper baseMaterialCodeMapMapper;

    @Mock
    private MaterialStatusStrategy materialStatusStrategy;

    @Mock
    private MaterialDuplicateCheckStrategy materialDuplicateCheckStrategy;

    @Mock
    private MaterialCodeMapStrategy materialCodeMapStrategy;

    @Mock
    private MaterialQueryStrategy materialQueryStrategy;

    private MaterialDomainServiceImpl service;

    @Captor
    private ArgumentCaptor<BaseMaterial> materialCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        MpTableInfoInit.init(BaseMaterial.class, BaseMaterialCodeMap.class);
    }

    @BeforeEach
    void setUp() {
        service = new MaterialDomainServiceImpl(baseMaterialMapper, baseMaterialCodeMapMapper,
            materialStatusStrategy, materialDuplicateCheckStrategy, materialCodeMapStrategy, materialQueryStrategy);
    }

    @Nested
    @DisplayName("requireMaterial：物料存在性校验")
    class RequireMaterialTest {

        @Test
        @DisplayName("物料不存在，抛 11002")
        void shouldThrow_whenMaterialMissing() {
            given(baseMaterialMapper.selectById(99L)).willReturn(null);

            assertThatThrownBy(() -> service.requireMaterial(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_NOT_FOUND.getCode()))
                .hasMessage("物料 99 不存在");
        }

        @Test
        @DisplayName("物料存在，返回实体")
        void shouldReturn_whenMaterialExists() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);
            given(baseMaterialMapper.selectById(1L)).willReturn(material);

            assertThat(service.requireMaterial(1L)).isSameAs(material);
        }

        @Test
        @DisplayName("按编码查询不存在，抛 11002")
        void shouldThrowByCode_whenMissing() {
            given(baseMaterialMapper.selectOne(any())).willReturn(null);

            assertThatThrownBy(() -> service.requireMaterialByCode("0103999999"))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_NOT_FOUND.getCode()))
                .hasMessage("物料 0103999999 不存在");
        }
    }

    @Nested
    @DisplayName("countByCategoryId：分类物料计数（跨域最小接口）")
    class CountByCategoryIdTest {

        @Test
        @DisplayName("有挂载物料返回计数")
        void shouldReturnCount_whenMaterialsMounted() {
            given(baseMaterialMapper.selectCount(any())).willReturn(5L);

            assertThat(service.countByCategoryId(3L)).isEqualTo(5L);
        }

        @Test
        @DisplayName("无挂载物料返回 0")
        void shouldReturnZero_whenNoMaterials() {
            given(baseMaterialMapper.selectCount(any())).willReturn(null);

            assertThat(service.countByCategoryId(3L)).isZero();
        }
    }

    @Nested
    @DisplayName("状态机流转编排")
    class TransitionTest {

        @Test
        @DisplayName("audit：策略流转后 patch 携带乐观锁版本落库")
        void shouldPersistPatchWithVersion_whenAudit() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);
            material.setVersion(3);
            given(baseMaterialMapper.selectById(1L)).willReturn(material);
            given(materialStatusStrategy.audit(material, true)).willReturn(1);
            given(baseMaterialMapper.updateById(any(BaseMaterial.class))).willReturn(1);

            Integer status = service.audit(1L, true);

            assertThat(status).isEqualTo(1);
            verify(baseMaterialMapper).updateById(materialCaptor.capture());
            BaseMaterial patch = materialCaptor.getValue();
            assertThat(patch.getId()).isEqualTo(1L);
            assertThat(patch.getStatus()).isEqualTo(1);
            assertThat(patch.getVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("disable：patch 同时携带停用原因（remark）")
        void shouldPersistReason_whenDisable() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 2);
            // 模拟状态机策略契约：停用原因落实体 remark
            material.setRemark("需求取消");
            given(baseMaterialMapper.selectById(1L)).willReturn(material);
            given(materialStatusStrategy.disable(material, "需求取消")).willReturn(3);
            given(baseMaterialMapper.updateById(any(BaseMaterial.class))).willReturn(1);

            Integer status = service.disable(1L, "需求取消");

            assertThat(status).isEqualTo(3);
            verify(baseMaterialMapper).updateById(materialCaptor.capture());
            assertThat(materialCaptor.getValue().getRemark()).isEqualTo("需求取消");
        }

        @Test
        @DisplayName("enable：策略抛 11010 时透传，不落库")
        void shouldRethrow_whenEnableIllegal() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);
            given(baseMaterialMapper.selectById(1L)).willReturn(material);
            given(materialStatusStrategy.enable(material))
                .willThrow(new BusinessException(BaseErrorCode.MATERIAL_NOT_AUDITED, "物料 0103000001 未审核，不可启用"));

            assertThatThrownBy(() -> service.enable(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_NOT_AUDITED.getCode()));
            verify(baseMaterialMapper, never()).updateById(any(BaseMaterial.class));
        }
    }

    @Nested
    @DisplayName("落库与委派")
    class PersistTest {

        @Test
        @DisplayName("updateRow：更新 0 行抛 10601（乐观锁冲突）")
        void shouldThrow_whenOptimisticLockConflict() {
            given(baseMaterialMapper.updateById(any(BaseMaterial.class))).willReturn(0);

            assertThatThrownBy(() -> service.updateRow(MaterialTestFactory.material(1L, "0103000001", "电阻", 0)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }

        @Test
        @DisplayName("insert/deleteById/listByIds 透传")
        void shouldDelegatePersistOps() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);
            service.insert(material);
            service.deleteById(1L);
            service.listByIds(List.of(1L));

            verify(baseMaterialMapper).insert(material);
            verify(baseMaterialMapper).deleteById(1L);
            verify(baseMaterialMapper).selectByIds(List.of(1L));
        }

        @Test
        @DisplayName("pageMaterials/listMaterials 委派查询策略")
        void shouldDelegateQueries() {
            Page<BaseMaterial> page = new Page<>(1, 20);
            given(materialQueryStrategy.pageMaterials(any(MaterialQueryDTO.class))).willReturn(page);

            assertThat(service.pageMaterials(new MaterialQueryDTO(null, null, null, null, null,
                null, null, null, null, null))).isSameAs(page);
            verify(materialQueryStrategy).pageMaterials(any(MaterialQueryDTO.class));
        }

        @Test
        @DisplayName("requireMappingAvailable/requireNewMaterialExists 委派旧码映射策略")
        void shouldDelegateCodeMapGuards() {
            BaseMaterial material = MaterialTestFactory.material(3L, "0103000001", "电阻", 0);
            given(materialCodeMapStrategy.requireNewMaterialExists(3L)).willReturn(material);

            assertThat(service.requireNewMaterialExists(3L)).isSameAs(material);
            service.requireMappingAvailable("IC-001", "采购部", null);

            verify(materialCodeMapStrategy).requireMappingAvailable("IC-001", "采购部", null);
            verify(materialCodeMapStrategy).requireNewMaterialExists(3L);
        }

        @Test
        @DisplayName("requireCodeAvailable/requireFourElementsAvailable 委派重复检测策略")
        void shouldDelegateDuplicateGuards() {
            service.requireCodeAvailable("0103000001", 1L);
            service.requireFourElementsAvailable("电阻", null, null, "个", 1L);

            verify(materialDuplicateCheckStrategy).requireCodeAvailable("0103000001", 1L);
            verify(materialDuplicateCheckStrategy).requireFourElementsAvailable("电阻", null, null, "个", 1L);
        }

        @Test
        @DisplayName("requireDeletable 委派状态机守卫（引用计数透传）")
        void shouldDelegateDeletable() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);

            assertThatCode(() -> service.requireDeletable(material, 2L)).doesNotThrowAnyException();

            verify(materialStatusStrategy).requireDeletable(material, 2L);
        }
    }
}
