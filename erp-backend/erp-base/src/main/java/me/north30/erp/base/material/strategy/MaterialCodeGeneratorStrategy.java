package me.north30.erp.base.material.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.spi.CodeGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 物料编码生成策略：经 erp-common {@link CodeGenerator} SPI 取号，
 * 编码 = 分类编码前 4 位 + 6 位流水（接口文档 6.2.4：2 位一级分类 + 2 位二级分类 + 6 位流水）。
 * <p>取号约定：bizType = material_code、prefix = 分类编码前 4 位（流水按前缀隔离递增）；
 * system 发号实现流水用尽抛"流水用尽"语义异常（SystemErrorCode.SEQUENCE_EXHAUSTED，码值 18044，
 * SPI 契约约定 base 域按 code 转译），本策略按 code 转译为物料域语义码 11014 后透出；
 * 其余业务异常（如并发取号冲突 10601）原样透传。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialCodeGeneratorStrategy {

    /** 物料编码取号业务类型（sys_code_sequence.biz_type） */
    public static final String BIZ_TYPE_MATERIAL_CODE = "material_code";

    /** 分类编码前缀长度（二级分类编码固定 4 位，决定物料编码前 4 位） */
    public static final int CATEGORY_PREFIX_LENGTH = 4;

    /** system 发号实现"流水用尽"语义码（SystemErrorCode.SEQUENCE_EXHAUSTED，SPI 契约约定按 code 转译） */
    static final int SEQUENCE_EXHAUSTED_SYSTEM_CODE = 18044;

    private final CodeGenerator codeGenerator;

    /**
     * 按分类编码生成物料编码：prefix = 分类编码前 4 位，流水由 system 发号实现按前缀隔离递增。
     *
     * @param categoryCode 分类编码（须为 4 位二级分类编码）
     * @return 物料编码（如 0103000001）
     * @throws BusinessException 分类编码不足 4 位（11004 语义）或 6 位流水用尽（11014）
     */
    public String generate(String categoryCode) {
        if (!StringUtils.hasText(categoryCode) || categoryCode.length() < CATEGORY_PREFIX_LENGTH) {
            throw new BusinessException(BaseErrorCode.MATERIAL_CODE_CATEGORY_MISMATCH,
                "分类编码 " + categoryCode + " 非 4 位二级分类编码，无法生成物料编码");
        }
        String prefix = categoryCode.substring(0, CATEGORY_PREFIX_LENGTH);
        try {
            String materialCode = codeGenerator.generate(BIZ_TYPE_MATERIAL_CODE, prefix);
            log.debug("物料编码生成成功：categoryCode={}, materialCode={}", categoryCode, materialCode);
            return materialCode;
        } catch (BusinessException e) {
            // 非吞异常：仅按 SPI 契约将"流水用尽"语义码转译为物料域 11014，其余原样透传
            if (e.getCode() == SEQUENCE_EXHAUSTED_SYSTEM_CODE) {
                log.warn("物料编码流水用尽：categoryCode={}", prefix);
                throw new BusinessException(BaseErrorCode.MATERIAL_CODE_EXHAUSTED,
                    "分类 " + prefix + " 下物料编码流水已用尽");
            }
            throw e;
        }
    }
}
