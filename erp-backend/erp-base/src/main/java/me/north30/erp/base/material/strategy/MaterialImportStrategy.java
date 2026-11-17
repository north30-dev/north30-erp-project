package me.north30.erp.base.material.strategy;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.common.MaterialDictKeys;
import me.north30.erp.base.material.converter.MaterialConverter;
import me.north30.erp.base.material.dto.MaterialCreateDTO;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.service.MaterialDomainService;
import me.north30.erp.base.materialcategory.service.MaterialCategoryDomainService;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.spi.DictValidator;
import me.north30.erp.common.util.CsvUtil;

/**
 * 物料导入策略：CSV 行解析与模板生成（IO 类）+ 导入单行装配落库（装配类，参照
 * UserAssembleStrategy/AccessAssembleStrategy 模式——策略可注入跨域 DomainService 最小接口）。
 * <p>四要素查重经预载索引内存完成（禁行级查库）；行级校验异常统一抛出，由应用服务导入循环采集。</p>
 * <p>CSV 列与导入模板一致（不含物料编码——编码由服务端按分类生成，已使用编码不复用）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialImportStrategy {

    /** 计量单位字典类型（sys_dict_type.dict_type，key 收口 {@link MaterialDictKeys}） */
    private static final String DICT_TYPE_UNIT = MaterialDictKeys.UNIT;

    /** 模板表头（与 CSV 导入列一一对应，带 * 为必填） */
    static final String[] HEADER = {
        "物料名称*", "规格型号", "品牌", "物料分类ID*", "物料类型*(1原材料/2半成品/3成品/4辅料)",
        "来源类型*(1自制/2外购/3外协)", "计量单位*", "图号", "图号版本", "标准成本", "安全库存",
        "最大库存", "最小采购批量", "ABC分类(A/B/C)", "采购提前期", "生产提前期", "计价方法(0)",
        "是否批次管理(true/false)", "是否序列号管理(true/false)", "是否保质期管理(true/false)",
        "保质期天数", "参与MRP(0/1)", "备注"
    };

    /** 导入单批行数上限（接口文档 6.2.11：单批 ≤10,000 行） */
    private static final int MAX_ROWS = 10_000;

    private static final DateTimeFormatter BATCH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MaterialDomainService materialDomainService;
    private final MaterialCategoryDomainService materialCategoryDomainService;
    private final MaterialConverter materialConverter;
    private final MaterialCodeGeneratorStrategy materialCodeGeneratorStrategy;
    private final DictValidator dictValidator;

    /**
     * 解析导入 CSV 为创建 DTO 列表（数据行从 1 开始，不含表头）。
     *
     * @param file 导入文件（CSV，UTF-8，首行表头）
     * @return 创建 DTO 列表
     * @throws BusinessException 文件为空/超行数上限/列数不符抛 10001
     */
    public List<MaterialCreateDTO> parse(MultipartFile file) {
        List<List<String>> csvRows = CsvUtil.readRows(readable(file));
        requireHeader(csvRows.isEmpty() ? List.of() : csvRows.get(0));
        List<MaterialCreateDTO> rows = new ArrayList<>();
        for (int i = 1; i < csvRows.size(); i++) {
            List<String> fields = csvRows.get(i);
            if (fields.stream().allMatch(f -> f == null || f.isBlank())) {
                continue;
            }
            if (rows.size() >= MAX_ROWS) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "导入行数超过上限 " + MAX_ROWS + " 行");
            }
            rows.add(toDTO(fields));
        }
        return rows;
    }

    /**
     * 生成导入模板 CSV 字节（UTF-8 BOM + 表头，委托公共 CsvUtil）。
     *
     * @return 模板文件内容字节
     */
    public byte[] template() {
        return CsvUtil.toCsvBytes(List.of(HEADER), List.of());
    }

    /**
     * 生成导入批次号（IMP + 时间戳 + 6 位随机段）。
     *
     * @return 导入批次号
     */
    public String nextImportBatch() {
        return "IMP" + BATCH_FORMATTER.format(LocalDateTime.now())
            + new SplittableRandom().nextInt(900000, 999999);
    }

    /**
     * 导入单行装配落库：必填字段守卫、启用叶子分类校验（11017/11005）、unit 字典校验（11016）、
     * 四要素内存查重（11008，预载索引）、保质期一致性守卫、编码生成（11004/11014 经编码策略）。
     *
     * @param row             单行创建 DTO
     * @param fourElementIndex 既有物料四要素索引（本方法成功落库后回写新行）
     * @return 落库后的物料实体
     * @throws BusinessException 行级校验失败
     */
    public BaseMaterial buildAndInsert(MaterialCreateDTO row, Map<String, String> fourElementIndex) {
        if (!StringUtils.hasText(row.materialName()) || row.categoryId() == null
            || row.materialType() == null || row.sourceType() == null || !StringUtils.hasText(row.unit())) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "必填字段缺失（名称/分类/类型/来源/单位）");
        }
        String categoryCode = materialCategoryDomainService.requireEnabledCategoryCode(row.categoryId());
        if (!materialCategoryDomainService.isLeaf(row.categoryId())) {
            throw new BusinessException(BaseErrorCode.MATERIAL_CATEGORY_NOT_LEAF, "物料分类须选择叶子节点");
        }
        dictValidator.requireEnabledItem(DICT_TYPE_UNIT, row.unit());
        String duplicateCode = fourElementIndex.get(
            MaterialDuplicateCheckStrategy.fourElementKey(row.materialName(), row.spec(), row.brand(), row.unit()));
        if (duplicateCode != null) {
            throw new BusinessException(BaseErrorCode.MATERIAL_DUPLICATED,
                "已存在相同物料（" + duplicateCode + "），请确认是否重复");
        }
        BaseMaterial material = materialConverter.create(row);
        material.requireShelfLifeConsistency();
        material.applyCreateDefault();
        material.setMaterialCode(materialCodeGeneratorStrategy.generate(categoryCode));
        materialDomainService.insert(material);
        fourElementIndex.put(MaterialDuplicateCheckStrategy.fourElementKey(
            row.materialName(), row.spec(), row.brand(), row.unit()), material.getMaterialCode());
        return material;
    }

    /**
     * 表头校验（列数一致即可，容错列名文案微调）。
     */
    private static void requireHeader(List<String> header) {
        if (header.size() != HEADER.length) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "导入模板列数不符，请下载最新模板（应为 " + HEADER.length + " 列）");
        }
    }

    /**
     * 打开上传文件输入流（空文件直接抛 10001，读取异常统一转业务异常）。
     */
    private static InputStream readable(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "导入文件为空");
            }
            return file.getInputStream();
        } catch (IOException e) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "导入文件读取失败：" + e.getMessage());
        }
    }

    /**
     * CSV 字段 → 创建 DTO（缺失列视为 null；默认科目/供应商/仓库/库位不参与 CSV 导入，恒为 null）。
     */
    private static MaterialCreateDTO toDTO(List<String> f) {
        return new MaterialCreateDTO(
            text(f, 0), text(f, 1), text(f, 2),
            longOrNull(f, 3), intOrNull(f, 4), intOrNull(f, 5), text(f, 6),
            text(f, 7), text(f, 8), text(f, 9), text(f, 10), text(f, 11), text(f, 12),
            text(f, 13), intOrNull(f, 14), intOrNull(f, 15), intOrNull(f, 16),
            null, null, null, null, null, null,
            boolOrNull(f, 17), boolOrNull(f, 18), boolOrNull(f, 19), intOrNull(f, 20),
            intOrNull(f, 21), text(f, 22));
    }

    private static String text(List<String> f, int i) {
        return i < f.size() && !f.get(i).isEmpty() ? f.get(i) : null;
    }

    private static Long longOrNull(List<String> f, int i) {
        String v = text(f, i);
        return v == null ? null : Long.parseLong(v);
    }

    private static Integer intOrNull(List<String> f, int i) {
        String v = text(f, i);
        return v == null ? null : Integer.parseInt(v);
    }

    private static Boolean boolOrNull(List<String> f, int i) {
        String v = text(f, i);
        if (v == null) {
            return null;
        }
        return Boolean.parseBoolean(v) || "1".equals(v) || "是".equals(v);
    }
}
