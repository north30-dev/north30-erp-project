package me.north30.erp.system.log.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.system.log.dto.AuditLogQueryParam;
import me.north30.erp.system.log.vo.AuditLogVO;
import org.apache.ibatis.annotations.Param;

/**
 * 审计日志只读查询 Mapper（接口文档 5.8.1/5.8.2）。
 * <p>独立于审计写入侧（SysAuditLog/AuditLogStore 由审计切面链路负责），
 * 查询列直接对照 schema.sql 的 sys_audit_log 表，SQL 见 mapper/AuditLogQueryMapper.xml。</p>
 */
public interface AuditLogQueryMapper {

    /**
     * 审计日志分页查询：单据号/操作人模糊、时间左闭右开，按操作时间倒序。
     */
    IPage<AuditLogVO> selectAuditLogPage(Page<AuditLogVO> page, @Param("param") AuditLogQueryParam param);

    /**
     * 审计日志详情查询：额外返回变更前后 JSON（未删除记录）。
     */
    AuditLogVO selectAuditLogById(@Param("id") Long id);
}
