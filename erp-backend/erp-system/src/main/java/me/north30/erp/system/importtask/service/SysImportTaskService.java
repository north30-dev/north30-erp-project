package me.north30.erp.system.importtask.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.importtask.vo.ImportResultVO;
import me.north30.erp.system.importtask.vo.ImportTaskVO;

/**
 * 批量导入任务查询服务（SYS-11）。
 * <p>D2 口径：schema.sql 的 76 张表中未建导入任务表（无 sys_import_task），
 * 写入能力属后续模块（物料/期初库存导入为 D3/D4 落地），本阶段仅提供查询接口骨架。</p>
 */
public interface SysImportTaskService {

    /**
     * 导入任务分页查询（D2 无任务表，返回空分页）。
     */
    PageResult<ImportTaskVO> pageTasks(Integer pageNum, Integer pageSize);

    /**
     * 导入结果查询（接口文档 5.10.2，权限点 system:import:detail）。
     */
    ImportResultVO getResult(String taskId);
}
