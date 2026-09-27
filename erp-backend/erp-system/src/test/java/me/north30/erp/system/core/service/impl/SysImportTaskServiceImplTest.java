package me.north30.erp.system.core.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SysImportTaskServiceImpl 单元测试：D2 无导入任务表的空实现口径。
 */
class SysImportTaskServiceImplTest {

    private SysImportTaskServiceImpl importTaskService;

    @BeforeEach
    void setUp() {
        importTaskService = new SysImportTaskServiceImpl();
    }

    @Test
    @DisplayName("任务列表：无任务表返回空分页（默认页参数）")
    void pageTasksReturnsEmptyPage() {
        PageResult<?> result = importTaskService.pageTasks(null, null);

        assertEquals(0, result.total());
        assertTrue(result.list().isEmpty());
        assertEquals(1, result.pageNum());
        assertEquals(20, result.pageSize());
    }

    @Test
    @DisplayName("任务列表：pageSize 超过 200 抛 10001")
    void pageTasksRejectsOversizePageSize() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> importTaskService.pageTasks(1, 201));
        assertEquals(10001, exception.getCode());
    }

    @Test
    @DisplayName("结果查询：任务号为空抛 10001")
    void getResultRejectsBlankTaskId() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> importTaskService.getResult(" "));
        assertEquals(10001, exception.getCode());
    }

    @Test
    @DisplayName("结果查询：无任务表时任务不存在（10503）")
    void getResultThrowsTaskNotFound() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> importTaskService.getResult("IM2026090001"));
        assertEquals(10503, exception.getCode());
    }
}
