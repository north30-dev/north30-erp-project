package me.north30.erp.base.unit.material.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.strategy.MaterialStatusStrategy;
import me.north30.erp.base.unit.material.MaterialTestFactory;
import me.north30.erp.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MaterialStatusStrategy} 纯单元测试：流转入口委派与删除前置守卫（11011/11009）。
 */
class MaterialStatusStrategyTest {

    private final MaterialStatusStrategy strategy = new MaterialStatusStrategy();

    @Nested
    @DisplayName("audit/enable/disable：流转入口")
    class TransitionTest {

        @Test
        @DisplayName("audit 委派实体流转并返回变更后状态")
        void shouldAudit_whenNew() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);

            Integer status = strategy.audit(material, true);

            assertThat(status).isEqualTo(1);
        }

        @Test
        @DisplayName("disable 委派实体流转，停用原因非空落 remark")
        void shouldPersistReason_whenDisableWithReason() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 2);

            Integer status = strategy.disable(material, "需求取消");

            assertThat(status).isEqualTo(3);
            assertThat(material.getRemark()).isEqualTo("需求取消");
        }

        @Test
        @DisplayName("disable 停用原因为空时不覆盖 remark")
        void shouldKeepRemark_whenReasonBlank() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 2);
            material.setRemark("既有备注");

            strategy.disable(material, " ");

            assertThat(material.getRemark()).isEqualTo("既有备注");
        }
    }

    @Nested
    @DisplayName("requireDeletable：删除前置守卫")
    class RequireDeletableTest {

        @Test
        @DisplayName("已被业务单据引用，抛 11011")
        void shouldThrow_whenReferenced() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);

            assertThatThrownBy(() -> strategy.requireDeletable(material, 3L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_REFERENCED.getCode()))
                .hasMessage("物料 0103000001 已被业务单据引用，不可删除");
        }

        @Test
        @DisplayName("非新建状态，抛 11009")
        void shouldThrow_whenNotNew() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 2);

            assertThatThrownBy(() -> strategy.requireDeletable(material, 0L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_ENABLED_UNDELETABLE.getCode()))
                .hasMessage("物料 0103000001 非新建状态（当前状态 2），不可删除");
        }

        @Test
        @DisplayName("新建且无引用，通过")
        void shouldPass_whenNewAndNotReferenced() {
            BaseMaterial material = MaterialTestFactory.material(1L, "0103000001", "电阻", 0);

            assertThatCode(() -> strategy.requireDeletable(material, 0L)).doesNotThrowAnyException();
        }
    }
}
