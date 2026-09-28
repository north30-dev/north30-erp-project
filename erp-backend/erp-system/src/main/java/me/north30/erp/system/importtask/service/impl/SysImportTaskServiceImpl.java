package me.north30.erp.system.importtask.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.importtask.service.SysImportTaskService;
import me.north30.erp.system.importtask.vo.ImportResultVO;
import me.north30.erp.system.importtask.vo.ImportTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 批量导入任务查询服务实现。
 * <p>D2 空实现口径：数据库无导入任务表，任务列表恒为空分页；结果查询按任务不存在处理（10503）。
 * 后续导入写入能力落地时（sys_import_task 建表）替换本实现即可。</p>
 */
@Slf4j
@Service
public class SysImportTaskServiceImpl implements SysImportTaskService {

    @Override
    public PageResult<ImportTaskVO> pageTasks(Integer pageNum, Integer pageSize) {
        long safePageNum = pageNum == null || pageNum < 1 ? PageConstants.DEFAULT_PAGE_NUM : pageNum;
        long safePageSize = pageSize == null || pageSize < 1
            ? PageConstants.DEFAULT_PAGE_SIZE
            : pageSize;
        if (safePageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "每页条数须在 1-200 之间");
        }
        return PageResult.empty(safePageNum, safePageSize);
    }

    @Override
    public ImportResultVO getResult(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "导入任务号不能为空");
        }
        throw new BusinessException(SystemManageErrorCode.IMPORT_TASK_NOT_FOUND);
    }
}
