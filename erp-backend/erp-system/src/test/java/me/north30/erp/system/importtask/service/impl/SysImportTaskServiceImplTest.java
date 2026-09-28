package me.north30.erp.system.importtask.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.common.enums.SystemManageErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SysImportTaskServiceImpl} 纯单元测试：D2 空实现口径（空分页 + 任务不存在）。
 * <p>被测类无任何外部依赖，无需 Mockito 桩；测试数据仅为基础类型入参，无需 TestFactory。</p>
 */
@ExtendWith(MockitoExtension.class)
class SysImportTaskServiceImplTest {

    @InjectMocks
    private SysImportTaskServiceImpl service;

    @Nested
    @DisplayName("pageTasks：分页查询导入任务")
    class PageTasksTest {

        @Test   
        @DisplayName("查询无导入任务时，返回空分页")
        void shouldReturnEmptyPage_whenNoTasks() {
            // Given：D2 阶段数据库无导入任务表（入参为合法分页参数）

            // When
            PageResult<?> result = service.pageTasks(1, 20);

            // Then：恒为空分页
            assertThat(result.total()).isZero();
            assertThat(result.list()).isEmpty();
            assertThat(result.pageNum()).isEqualTo(1L);
            assertThat(result.pageSize()).isEqualTo(20L);
            assertThat(result.pages()).isZero();
        }

        @Test   
        @DisplayName("查询导入任务时，归一化分页参数")
        void shouldNormalizeParams_whenInvalid() {
            // Given：页码小于 1 且每页条数小于 1
            // When
            PageResult<?> result = service.pageTasks(0, -5);

            // Then：分页参数归位为默认值
            assertThat(result.pageNum()).isEqualTo(1L);
            assertThat(result.pageSize()).isEqualTo(20L);
            assertThat(result.total()).isZero();
        }

        @Test   
        @DisplayName("查询导入任务时，每页条数超过上限 200 抛出异常")
        void shouldThrow_whenPageSizeExceedsMax() {
            // Given：每页条数 201 超过上限 200
            // When + Then
            assertThatThrownBy(() -> service.pageTasks(1, 201))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("每页条数须在 1-200 之间");
        }
    }

    @Nested
    class GetResultTest {

        @Test   
        @DisplayName("查询导入任务结果，任务号为空时抛出异常")
        void shouldThrow_whenTaskIdBlank() {
            // Given：任务号为空白字符串
            // When + Then
            assertThatThrownBy(() -> service.getResult(" "))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("导入任务号不能为空");
        }
        
        @Test   
        @DisplayName("查询不存在的任务时，抛出异常")
        void shouldThrow_whenTaskNotFound() {
            // Given：D2 阶段任务数据不存在（非空任务号）
            // When + Then
            assertThatThrownBy(() -> service.getResult("TASK-001"))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.IMPORT_TASK_NOT_FOUND.getCode()))
                .hasMessage("导入任务不存在");
        }
    }
}
