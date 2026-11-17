package me.north30.erp.base.unit.material.entity;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.unit.material.MaterialTestFactory;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BaseMaterial} 局部充血纯单元测试：创建默认值、状态机（审核/启用/停用）、
 * 保质期一致性守卫。
 */
class BaseMaterialTest {

    @Nested
    @DisplayName("applyCreateDefault：创建默认值")
    class ApplyCreateDefaultTest {

        @Test
        @DisplayName("null 字段填充缺省值并落初始状态 0-新建")
        void shouldFillDefaults_whenFieldsNull() {
            BaseMaterial material = new BaseMaterial();

            material.applyCreateDefault();

            assertThat(material.getStandardCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(material.getSafetyStock()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(material.getMaxStock()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(material.getMinOrderQty()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(material.getAbcClass()).isEqualTo("C");
            assertThat(material.getLeadTime()).isZero();
            assertThat(material.getProductionLeadTime()).isZero();
            assertThat(material.getCostMethod()).isZero();
            assertThat(material.getIsBatchManaged()).isFalse();
            assertThat(material.getIsSerialManaged()).isFalse();
            assertThat(material.getIsShelfLife()).isFalse();
            assertThat(material.getIsMrpActive()).isEqualTo(1);
            assertThat(material.getStatus()).isZero();
        }

        @Test
        @DisplayName("显式传入值不覆盖")
        void shouldKeepExplicitValues_whenFieldsProvided() {
            BaseMaterial material = new BaseMaterial();
            material.setAbcClass("A");
            material.setMinOrderQty(new BigDecimal("5"));

            material.applyCreateDefault();

            assertThat(material.getAbcClass()).isEqualTo("A");
            assertThat(material.getMinOrderQty()).isEqualByComparingTo(new BigDecimal("5"));
        }
    }

    @Nested
    @DisplayName("audit：审核流转")
    class AuditTest {

        @Test
        @DisplayName("新建物料审核通过，0 → 1")
        void shouldTransitionToAudited_whenApproved() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);

            material.audit(true);

            assertThat(material.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("审核驳回保持 0-新建")
        void shouldStayNew_whenRejected() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);

            material.audit(false);

            assertThat(material.getStatus()).isZero();
        }

        @Test
        @DisplayName("已审核物料再审核抛 11010，状态不变")
        void shouldThrow_whenAlreadyAudited() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 1);

            assertThatThrownBy(() -> material.audit(true))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_NOT_AUDITED.getCode()));
            assertThat(material.getStatus()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("enable：启用流转")
    class EnableTest {

        @Test
        @DisplayName("审核物料启用，1 → 2")
        void shouldTransitionToEnabled_whenAudited() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 1);

            material.enable();

            assertThat(material.getStatus()).isEqualTo(2);
        }

        @Test
        @DisplayName("新建物料直接启用抛 11010")
        void shouldThrow_whenNew() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);

            assertThatThrownBy(material::enable)
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_NOT_AUDITED.getCode()));
        }

        @Test
        @DisplayName("停用物料直接启用抛 11003")
        void shouldThrow_whenDisabled() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 3);

            assertThatThrownBy(material::enable)
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_DISABLED.getCode()));
        }

        @Test
        @DisplayName("已启用物料幂等通过")
        void shouldPass_whenAlreadyEnabled() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 2);

            assertThatCode(material::enable).doesNotThrowAnyException();
            assertThat(material.getStatus()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("disable：停用流转")
    class DisableTest {

        @Test
        @DisplayName("审核/启用物料停用，1/2 → 3")
        void shouldTransitionToDisabled_whenAuditedOrEnabled() {
            BaseMaterial audited = MaterialTestFactory.material(1L, "0103000001", "电阻", 1);
            BaseMaterial enabled = MaterialTestFactory.material(2L, "0103000002", "电容", 2);

            audited.disable();
            enabled.disable();

            assertThat(audited.getStatus()).isEqualTo(3);
            assertThat(enabled.getStatus()).isEqualTo(3);
        }

        @Test
        @DisplayName("新建物料直接停用抛 11010")
        void shouldThrow_whenNew() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);

            assertThatThrownBy(material::disable)
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_NOT_AUDITED.getCode()));
        }

        @Test
        @DisplayName("已停用物料幂等通过")
        void shouldPass_whenAlreadyDisabled() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 3);

            assertThatCode(material::disable).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("requireShelfLifeConsistency：保质期一致性守卫")
    class ShelfLifeTest {

        @Test
        @DisplayName("开启保质期管理但未填天数，抛 10001")
        void shouldThrow_whenShelfLifeWithoutDays() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);
            material.setIsShelfLife(true);
            material.setShelfLifeDays(null);

            assertThatThrownBy(material::requireShelfLifeConsistency)
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("物料 电阻 开启保质期管理时须填写保质期天数");
        }

        @Test
        @DisplayName("开关关闭或天数齐全时通过")
        void shouldPass_whenConsistent() {
            BaseMaterial withoutFlag = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);
            BaseMaterial withDays = MaterialTestFactory.material(2L, "0103000002", "电容", 0);
            withDays.setIsShelfLife(true);
            withDays.setShelfLifeDays(365);

            assertThatCode(withoutFlag::requireShelfLifeConsistency).doesNotThrowAnyException();
            assertThatCode(withDays::requireShelfLifeConsistency).doesNotThrowAnyException();
        }
    }
}
