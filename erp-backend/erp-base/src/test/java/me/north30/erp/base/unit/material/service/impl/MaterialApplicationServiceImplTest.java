package me.north30.erp.base.unit.material.service.impl;

import java.util.List;
import java.util.Map;

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
import org.springframework.mock.web.MockMultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.converter.MaterialConverter;
import me.north30.erp.base.material.converter.MaterialConverterImpl;
import me.north30.erp.base.material.dto.MaterialCreateDTO;
import me.north30.erp.base.material.dto.MaterialQueryDTO;
import me.north30.erp.base.material.dto.MaterialUpdateDTO;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.service.impl.MaterialApplicationServiceImpl;
import me.north30.erp.base.material.service.MaterialDomainService;
import me.north30.erp.base.material.strategy.MaterialCodeGeneratorStrategy;
import me.north30.erp.base.material.strategy.MaterialImportStrategy;
import me.north30.erp.base.material.vo.MaterialBatchDeleteVO;
import me.north30.erp.base.material.vo.MaterialCreateVO;
import me.north30.erp.base.material.vo.MaterialDeleteVO;
import me.north30.erp.base.material.vo.MaterialImportResultVO;
import me.north30.erp.base.material.vo.MaterialStatusVO;
import me.north30.erp.base.material.vo.MaterialVO;
import me.north30.erp.base.materialcategory.service.MaterialCategoryDomainService;
import me.north30.erp.base.unit.material.MaterialTestFactory;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.spi.DictOptionsResolver;
import me.north30.erp.common.spi.DictValidator;
import me.north30.erp.common.util.CsvUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link MaterialApplicationServiceImpl} 纯单元测试：新增/修改编排（跨域分类校验与字典校验）、
 * 删除与批量删除（部分失败语义）、状态机流转委派、导入（SKIP_ERROR/ALL_OR_NOTHING）、
 * 分页（分类名称批量回填）、旧码映射编排。
 * <p>Converter 使用 MapStruct 生成实现（{@link MaterialConverterImpl} 真实实例），映射路径与生产一致。</p>
 */
@ExtendWith(MockitoExtension.class)
class MaterialApplicationServiceImplTest {

    private static final long CATEGORY_ID = 3L;

    @Mock
    private MaterialDomainService materialDomainService;

    @Mock
    private MaterialCategoryDomainService materialCategoryDomainService;

    @Mock
    private MaterialCodeGeneratorStrategy materialCodeGeneratorStrategy;

    @Mock
    private MaterialImportStrategy materialImportStrategy;

    @Mock
    private DictValidator dictValidator;

    @Mock
    private DictOptionsResolver dictOptionsResolver;

    private final MaterialConverter materialConverter = new MaterialConverterImpl();

    private MaterialApplicationServiceImpl service;

    @Captor
    private ArgumentCaptor<BaseMaterial> materialCaptor;

    @BeforeEach
    void setUp() {
        service = new MaterialApplicationServiceImpl(materialDomainService, materialCategoryDomainService,
            materialConverter, materialCodeGeneratorStrategy, materialImportStrategy, dictValidator,
            dictOptionsResolver);
    }

    private void givenCategoryUsable() {
        given(materialCategoryDomainService.requireEnabledCategoryCode(CATEGORY_ID)).willReturn("0103");
        given(materialCategoryDomainService.isLeaf(CATEGORY_ID)).willReturn(true);
    }

    @Nested
    @DisplayName("create：新增物料编排")
    class CreateTest {

        @Test
        @DisplayName("分类启用叶子 + 单位合法 + 四要素不重复，编码服务端生成，状态 0-新建")
        void shouldCreateWithGeneratedCode_whenAllGuardsPass() {
            givenCategoryUsable();
            given(materialCodeGeneratorStrategy.generate("0103")).willReturn("0103000001");
            willAnswer(invocation -> {
                invocation.getArgument(0, BaseMaterial.class).setId(10L);
                return null;
            }).given(materialDomainService).insert(any(BaseMaterial.class));

            MaterialCreateVO vo = service.create(MaterialTestFactory.createDTO("电阻"));

            assertThat(vo.id()).isEqualTo(10L);
            assertThat(vo.materialCode()).isEqualTo("0103000001");
            assertThat(vo.status()).isZero();
            verify(materialDomainService).insert(materialCaptor.capture());
            assertThat(materialCaptor.getValue().getMaterialCode()).isEqualTo("0103000001");
            assertThat(materialCaptor.getValue().getStandardCost()).isNotNull();
            verify(dictValidator).requireEnabledItem("unit", "个");
        }

        @Test
        @DisplayName("分类非叶子，抛 11005，不落库")
        void shouldThrow_whenCategoryNotLeaf() {
            given(materialCategoryDomainService.requireEnabledCategoryCode(CATEGORY_ID)).willReturn("0103");
            given(materialCategoryDomainService.isLeaf(CATEGORY_ID)).willReturn(false);

            assertThatThrownBy(() -> service.create(MaterialTestFactory.createDTO("电阻")))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_CATEGORY_NOT_LEAF.getCode()))
                .hasMessage("物料分类须选择叶子节点");
            verify(materialDomainService, never()).insert(any(BaseMaterial.class));
        }

        @Test
        @DisplayName("分类已停用/不存在，异常透传，不落库")
        void shouldRethrow_whenCategoryDisabled() {
            willThrow(new BusinessException(BaseErrorCode.MATERIAL_CATEGORY_DISABLED, "物料分类 芯片 已停用"))
                .given(materialCategoryDomainService).requireEnabledCategoryCode(CATEGORY_ID);

            assertThatThrownBy(() -> service.create(MaterialTestFactory.createDTO("电阻")))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_CATEGORY_DISABLED.getCode()));
            verify(materialDomainService, never()).insert(any(BaseMaterial.class));
        }

        @Test
        @DisplayName("计量单位不在字典 unit 内，11016 透传")
        void shouldRethrow_whenUnitInvalid() {
            givenCategoryUsable();
            willThrow(new BusinessException(BaseErrorCode.MATERIAL_UNIT_INVALID, "计量单位 xx 非法，请从字典中选择"))
                .given(dictValidator).requireEnabledItem("unit", "个");

            assertThatThrownBy(() -> service.create(MaterialTestFactory.createDTO("电阻")))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_UNIT_INVALID.getCode()));
        }

        @Test
        @DisplayName("四要素重复，11008 透传，不落库")
        void shouldRethrow_whenFourElementsDuplicated() {
            givenCategoryUsable();
            willThrow(new BusinessException(BaseErrorCode.MATERIAL_DUPLICATED, "已存在相同物料（0103000002），请确认是否重复"))
                .given(materialDomainService).requireFourElementsAvailable(any(), any(), any(), any(), any());

            assertThatThrownBy(() -> service.create(MaterialTestFactory.createDTO("电阻")))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_DUPLICATED.getCode()));
            verify(materialDomainService, never()).insert(any(BaseMaterial.class));
        }
    }

    @Nested
    @DisplayName("update：修改物料编排（部分更新）")
    class UpdateTest {

        @Test
        @DisplayName("分类/单位变更走跨域校验，patch 携带版本与更新时间落库")
        void shouldUpdate_whenCategoryAndUnitChanged() {
            BaseMaterial current = MaterialTestFactory.material(1L, "0103000001", "电阻", 2);
            given(materialDomainService.requireMaterial(1L)).willReturn(current);
            givenCategoryUsable();

            String updateTime = service.update(1L, MaterialTestFactory.updateDTO("电阻", CATEGORY_ID, "个", 4));

            assertThat(updateTime).isNotBlank();
            verify(materialDomainService).updateRow(materialCaptor.capture());
            BaseMaterial patch = materialCaptor.getValue();
            assertThat(patch.getId()).isEqualTo(1L);
            assertThat(patch.getMaterialName()).isEqualTo("电阻");
            assertThat(patch.getVersion()).isEqualTo(4);
            // 编码不可修改（Converter 无映射即不拷贝）
            assertThat(patch.getMaterialCode()).isNull();
            verify(materialDomainService).requireFourElementsAvailable(
                eq("电阻"), eq(null), eq(null), eq("个"), eq(1L));
        }

        @Test
        @DisplayName("计价方法变更（本期仅支持 0），抛 11012")
        void shouldThrow_whenCostMethodChanged() {
            given(materialDomainService.requireMaterial(1L))
                .willReturn(MaterialTestFactory.material(1L, "0103000001", "电阻", 2));
            MaterialUpdateDTO dto = new MaterialUpdateDTO(null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, 1,
                null, null, null, null, null, null,
                null, null, null, null, null, null, 1);

            assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_COST_METHOD_IMMUTABLE.getCode()));
        }

        @Test
        @DisplayName("乐观锁冲突（更新 0 行），抛 10601")
        void shouldThrow_whenOptimisticLockConflict() {
            given(materialDomainService.requireMaterial(1L))
                .willReturn(MaterialTestFactory.material(1L, "0103000001", "电阻", 2));
            willThrow(new OptimisticLockingFailureException("数据已被其他操作修改，请重试"))
                .given(materialDomainService).updateRow(any(BaseMaterial.class));

            assertThatThrownBy(() -> service.update(1L, MaterialTestFactory.updateDTO(null, null, null, 9)))
                .isInstanceOf(OptimisticLockingFailureException.class);
        }
    }

    @Nested
    @DisplayName("delete/batchDelete：删除编排")
    class DeleteTest {

        @Test
        @DisplayName("新建物料删除成功，返回逻辑删除标记")
        void shouldDelete_whenNew() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);
            given(materialDomainService.requireMaterial(1L)).willReturn(material);

            MaterialDeleteVO vo = service.delete(1L);

            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.isDeleted()).isEqualTo(1);
            verify(materialDomainService).requireDeletable(material, 0L);
            verify(materialDomainService).deleteById(1L);
        }

        @Test
        @DisplayName("批量删除：不存在行采 11002、已引用行采 11009，成功行照常删除")
        void shouldCollectFailures_whenBatchDeletePartial() {
            BaseMaterial ok = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);
            BaseMaterial referenced = MaterialTestFactory.material(2L, "0103000002", "电容", 2);
            given(materialDomainService.listByIds(List.of(1L, 2L, 3L))).willReturn(List.of(ok, referenced));
            willDoNothing().given(materialDomainService).requireDeletable(ok, 0L);
            willThrow(new BusinessException(BaseErrorCode.MATERIAL_ENABLED_UNDELETABLE, "物料 0103000002 已启用，不可删除"))
                .given(materialDomainService).requireDeletable(referenced, 0L);

            MaterialBatchDeleteVO vo = service.batchDelete(MaterialTestFactory.batchDeleteDTO(List.of(1L, 2L, 3L)));

            assertThat(vo.successCount()).isEqualTo(1);
            assertThat(vo.failCount()).isEqualTo(2);
            assertThat(vo.failures()).extracting(MaterialBatchDeleteVO.Failure::errorCode)
                .containsExactly(BaseErrorCode.MATERIAL_ENABLED_UNDELETABLE.getCode(),
                    BaseErrorCode.MATERIAL_NOT_FOUND.getCode());
            verify(materialDomainService).deleteById(1L);
            verify(materialDomainService, never()).deleteById(2L);
            verify(materialDomainService, never()).deleteById(3L);
        }
    }

    @Nested
    @DisplayName("audit/enable/disable：状态机流转委派")
    class TransitionTest {

        @Test
        @DisplayName("audit 通过返回变更后状态（affectedBomCount 为 null）")
        void shouldAudit() {
            given(materialDomainService.audit(1L, true)).willReturn(1);

            MaterialStatusVO vo = service.audit(1L, MaterialTestFactory.auditDTO(true));

            assertThat(vo.status()).isEqualTo(1);
            assertThat(vo.affectedBomCount()).isNull();
        }

        @Test
        @DisplayName("disable 返回变更后状态与 BOM 提示计数（BOM 域未落地恒为 0）")
        void shouldDisable() {
            given(materialDomainService.disable(1L, "需求取消")).willReturn(3);

            MaterialStatusVO vo = service.disable(1L, MaterialTestFactory.disableDTO("需求取消"));

            assertThat(vo.status()).isEqualTo(3);
            assertThat(vo.affectedBomCount()).isZero();
        }

        @Test
        @DisplayName("enable 非法流转（11010）透传")
        void shouldRethrow_whenEnableIllegal() {
            willThrow(new BusinessException(BaseErrorCode.MATERIAL_NOT_AUDITED, "物料 0103000001 未审核，不可启用"))
                .given(materialDomainService).enable(1L);

            assertThatThrownBy(() -> service.enable(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_NOT_AUDITED.getCode()));
        }
    }

    @Nested
    @DisplayName("page：分页查询编排（分类名称批量回填）")
    class PageTest {

        @Test
        @DisplayName("分类名称一次 IN 批量查询后回填 VO")
        void shouldFillCategoryNames_whenPage() {
            Page<BaseMaterial> page = new Page<>(1, 20);
            page.setRecords(List.of(
                MaterialTestFactory.materialWithCost(1L, "0103000001", "0.25"),
                MaterialTestFactory.materialWithCost(2L, "0103000002", "0.5")));
            page.setTotal(2);
            given(materialDomainService.pageMaterials(any(MaterialQueryDTO.class))).willReturn(page);
            given(materialCategoryDomainService.getCategoryNameMap(any())).willReturn(Map.of(3L, "芯片"));

            var result = service.page(new MaterialQueryDTO(null, null, null, null, null, null, null, null, 1, 20));

            assertThat(result.total()).isEqualTo(2);
            assertThat(result.list()).extracting(MaterialVO::categoryName)
                .containsOnly("芯片");
            assertThat(result.list()).extracting(MaterialVO::standardCost)
                .containsExactly("0.250000", "0.500000");
        }
    }

    @Nested
    @DisplayName("importMaterials：批量导入编排")
    class ImportTest {

        private final MockMultipartFile file = new MockMultipartFile("file", new byte[0]);

        @Test
        @DisplayName("SKIP_ERROR：错误行采集，成功行照常入库")
        void shouldCollectErrors_whenSkipError() {
            List<MaterialCreateDTO> rows = List.of(
                MaterialTestFactory.createDTO("电阻"), MaterialTestFactory.createDTO("电容"));
            given(materialImportStrategy.parse(any())).willReturn(rows);
            given(materialDomainService.loadFourElementIndex()).willReturn(Map.of());
            willAnswer(invocation -> MaterialTestFactory.material(10L, "0103000001", "电阻", 0))
                .given(materialImportStrategy).buildAndInsert(eq(rows.get(0)), anyMap());
            willThrow(new BusinessException(BaseErrorCode.MATERIAL_DUPLICATED, "已存在相同物料（0103000002），请确认是否重复"))
                .given(materialImportStrategy).buildAndInsert(eq(rows.get(1)), anyMap());

            MaterialImportResultVO vo = service.importMaterials(file, "SKIP_ERROR");

            assertThat(vo.totalRows()).isEqualTo(2);
            assertThat(vo.successRows()).isEqualTo(1);
            assertThat(vo.failRows()).isEqualTo(1);
            assertThat(vo.errors()).extracting(MaterialImportResultVO.RowError::rowNo).containsExactly(2);
            assertThat(vo.errors()).extracting(MaterialImportResultVO.RowError::errorCode)
                .containsExactly(BaseErrorCode.MATERIAL_DUPLICATED.getCode());
        }

        @Test
        @DisplayName("ALL_OR_NOTHING（缺省）：任一行错误整批抛出回滚")
        void shouldThrowAllOrNothing_whenDefaultMode() {
            List<MaterialCreateDTO> rows = List.of(
                MaterialTestFactory.createDTO("电阻"), MaterialTestFactory.createDTO("电容"));
            given(materialImportStrategy.parse(any())).willReturn(rows);
            given(materialDomainService.loadFourElementIndex()).willReturn(Map.of());
            willAnswer(invocation -> MaterialTestFactory.material(10L, "0103000001", "电阻", 0))
                .given(materialImportStrategy).buildAndInsert(eq(rows.get(0)), anyMap());
            willThrow(new BusinessException(CommonErrorCode.PARAM_ERROR, "必填字段缺失（名称/分类/类型/来源/单位）"))
                .given(materialImportStrategy).buildAndInsert(eq(rows.get(1)), anyMap());

            assertThatThrownBy(() -> service.importMaterials(file, null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessageContaining("导入整批回滚")
                .hasMessageContaining("第 2 行");
        }
    }

    @Nested
    @DisplayName("exportCsv/downloadImportTemplate：导出与模板")
    class ExportTest {

        @Test
        @DisplayName("导出 CSV 含 BOM 且为字节流（委托公共 CsvUtil；类型/状态文案经字典 label 渲染）")
        void shouldExportCsv_whenMaterialsExist() {
            given(materialDomainService.listMaterials(any(MaterialQueryDTO.class))).willReturn(
                List.of(MaterialTestFactory.materialWithCost(1L, "0103000001", "0.25")));
            given(materialCategoryDomainService.getCategoryNameMap(any())).willReturn(Map.of(3L, "芯片"));
            given(dictOptionsResolver.loadItemLabels("material_type")).willReturn(Map.of("1", "原材料"));
            given(dictOptionsResolver.loadItemLabels("source_type")).willReturn(Map.of("2", "外购"));
            given(dictOptionsResolver.loadItemLabels("material_status")).willReturn(Map.of("2", "启用"));

            byte[] csv = service.exportCsv(new MaterialQueryDTO(null, null, null, null, null, null, null, null, 1, 20));

            assertThat(csv.length).isPositive();
            assertThat(new String(csv, java.nio.charset.StandardCharsets.UTF_8)).startsWith(CsvUtil.BOM)
                .contains("0103000001").contains("芯片")
                .contains("原材料").contains("外购").contains("启用");
        }

        @Test
        @DisplayName("字典缺失该项时回退默认文案兜底")
        void shouldFallbackToDefaultText_whenDictLabelsMissing() {
            given(materialDomainService.listMaterials(any(MaterialQueryDTO.class))).willReturn(
                List.of(MaterialTestFactory.materialWithCost(1L, "0103000001", "0.25")));
            given(materialCategoryDomainService.getCategoryNameMap(any())).willReturn(Map.of(3L, "芯片"));
            given(dictOptionsResolver.loadItemLabels(anyString())).willReturn(Map.of());

            byte[] csv = service.exportCsv(new MaterialQueryDTO(null, null, null, null, null, null, null, null, 1, 20));

            assertThat(new String(csv, java.nio.charset.StandardCharsets.UTF_8))
                .contains("原材料").contains("外购").contains("启用");
        }

        @Test
        @DisplayName("模板下载透传导入策略")
        void shouldDelegateTemplate() {
            given(materialImportStrategy.template()).willReturn(new byte[]{1});

            assertThat(service.downloadImportTemplate()).containsExactly((byte) 1);
        }
    }

    @Nested
    @DisplayName("createCodeMap：旧码映射编排")
    class CreateCodeMapTest {

        @Test
        @DisplayName("新物料存在 + 映射唯一，new_material_code 服务端回填")
        void shouldCreateCodeMap_whenGuardsPass() {
            BaseMaterial newMaterial = MaterialTestFactory.material(3L, "0103000001", "电阻", 0);
            given(materialDomainService.requireNewMaterialExists(3L)).willReturn(newMaterial);
            willAnswer(invocation -> {
                invocation.getArgument(0, me.north30.erp.base.material.entity.BaseMaterialCodeMap.class)
                    .setId(20L);
                return null;
            }).given(materialDomainService).insertCodeMap(any(
                me.north30.erp.base.material.entity.BaseMaterialCodeMap.class));

            var vo = service.createCodeMap(MaterialTestFactory.codeMapCreateDTO("IC-001", "采购部", 3L));

            assertThat(vo.id()).isEqualTo(20L);
            assertThat(vo.newMaterialCode()).isEqualTo("0103000001");
            verify(materialDomainService).requireMappingAvailable("IC-001", "采购部", null);
        }

        @Test
        @DisplayName("映射已存在（11015）透传，不落库")
        void shouldRethrow_whenMappingExists() {
            given(materialDomainService.requireNewMaterialExists(3L))
                .willReturn(MaterialTestFactory.material(3L, "0103000001", "电阻", 0));
            willThrow(new BusinessException(BaseErrorCode.CODE_MAP_EXISTS, "旧物料编码 IC-001（采购部）映射已存在"))
                .given(materialDomainService).requireMappingAvailable("IC-001", "采购部", null);

            assertThatThrownBy(() -> service.createCodeMap(MaterialTestFactory.codeMapCreateDTO("IC-001", "采购部", 3L)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.CODE_MAP_EXISTS.getCode()));
            verify(materialDomainService, never()).insertCodeMap(any(
                me.north30.erp.base.material.entity.BaseMaterialCodeMap.class));
        }
    }
}
