package me.north30.erp.base.unit.material.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.strategy.MaterialCodeGeneratorStrategy;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.spi.CodeGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link MaterialCodeGeneratorStrategy} 纯单元测试：前缀截取与取号透传、
 * 分类编码格式守卫（11004 语义）、"流水用尽"语义码转译（18044 → 11014）、其他业务异常透传。
 * <p>erp-base 不依赖 erp-system，18044 按裸码构造模拟 system 侧异常（SPI 契约约定的转译触发码）。</p>
 */
@ExtendWith(MockitoExtension.class)
class MaterialCodeGeneratorStrategyTest {

    /** system 发号实现"流水用尽"语义码（SystemErrorCode.SEQUENCE_EXHAUSTED） */
    private static final int SEQUENCE_EXHAUSTED = 18044;

    /** 通用可重试语义码（并发取号冲突） */
    private static final int OPTIMISTIC_LOCK_CONFLICT = 10601;

    @Mock
    private CodeGenerator codeGenerator;

    private MaterialCodeGeneratorStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MaterialCodeGeneratorStrategy(codeGenerator);
    }

    @Nested
    @DisplayName("generate：编码生成")
    class GenerateTest {

        @Test
        @DisplayName("按分类编码前 4 位取号并透传生成结果")
        void shouldGenerateWithPrefix_whenCategoryCodeValid() {
            given(codeGenerator.generate("material_code", "0103")).willReturn("0103000001");

            String materialCode = strategy.generate("0103");

            assertThat(materialCode).isEqualTo("0103000001");
            verify(codeGenerator).generate("material_code", "0103");
        }

        @Test
        @DisplayName("分类编码不足 4 位，抛 11004 语义，不取号")
        void shouldThrow_whenCategoryCodeTooShort() {
            assertThatThrownBy(() -> strategy.generate("010"))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode())
                        .isEqualTo(BaseErrorCode.MATERIAL_CODE_CATEGORY_MISMATCH.getCode()))
                .hasMessage("分类编码 010 非 4 位二级分类编码，无法生成物料编码");
            verify(codeGenerator, never()).generate(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("流水用尽转译（SPI 契约：18044 → 11014）")
    class ExhaustedTranslateTest {

        @Test
        @DisplayName("system 发号抛 18044（流水用尽），转译为 11014")
        void shouldTranslateTo11014_whenSequenceExhausted() {
            given(codeGenerator.generate(anyString(), anyString()))
                .willThrow(new BusinessException(SEQUENCE_EXHAUSTED, "编号流水已用尽"));

            assertThatThrownBy(() -> strategy.generate("0103"))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_CODE_EXHAUSTED.getCode()))
                .hasMessage("分类 0103 下物料编码流水已用尽");
        }

        @Test
        @DisplayName("其他业务异常（如并发取号冲突 10601）原样透传")
        void shouldRethrow_whenOtherBusinessException() {
            given(codeGenerator.generate(anyString(), anyString()))
                .willThrow(new BusinessException(OPTIMISTIC_LOCK_CONFLICT, "数据已被其他操作修改，请重试"));

            assertThatThrownBy(() -> strategy.generate("0103"))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(OPTIMISTIC_LOCK_CONFLICT));
        }
    }
}
