package me.north30.erp.system.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.log.dto.AuditLogQueryDTO;
import me.north30.erp.system.log.dto.AuditLogQueryParam;
import me.north30.erp.system.log.dto.LoginLogQueryDTO;
import me.north30.erp.system.log.entity.SysLoginLog;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.log.converter.LoginLogConverter;
import me.north30.erp.system.log.mapper.AuditLogQueryMapper;
import me.north30.erp.system.log.mapper.SysLoginLogMapper;
import me.north30.erp.system.log.service.SysLogQueryService;
import me.north30.erp.system.log.vo.AuditLogVO;
import me.north30.erp.system.log.vo.LoginLogVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * 日志查询服务实现：审计日志走只读 XML 查询（与审计写入侧解耦），登录日志走单表 LambdaQueryWrapper。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLogQueryServiceImpl implements SysLogQueryService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final AuditLogQueryMapper auditLogQueryMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final LoginLogConverter loginLogConverter;

    @Override
    @Transactional(readOnly = true)
    public PageResult<AuditLogVO> pageAuditLogs(AuditLogQueryDTO query) {
        long pageNum = normalizePageNum(query.pageNum());
        long pageSize = normalizePageSize(query.pageSize());
        Page<AuditLogVO> page = new Page<>(pageNum, pageSize);
        auditLogQueryMapper.selectAuditLogPage(page, buildAuditLogQueryParam(query));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogVO getAuditLogDetail(Long id) {
        AuditLogVO detail = auditLogQueryMapper.selectAuditLogById(id);
        if (detail == null) {
            throw new BusinessException(SystemManageErrorCode.AUDIT_LOG_NOT_FOUND);
        }
        return detail.withDiffFields(resolveDiffFields(detail.beforeJson(), detail.afterJson()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<LoginLogVO> pageLoginLogs(LoginLogQueryDTO query) {
        long pageNum = normalizePageNum(query.pageNum());
        long pageSize = normalizePageSize(query.pageSize());
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<SysLoginLog>()
            .like(StringUtils.hasText(query.username()), SysLoginLog::getUsername, query.username())
            .eq(query.loginType() != null, SysLoginLog::getLoginType, query.loginType())
            .eq(query.resultStatus() != null, SysLoginLog::getResultStatus, query.resultStatus())
            .orderByDesc(SysLoginLog::getLoginTime)
            .orderByDesc(SysLoginLog::getId);
        LocalDateTime timeStart = parseTime(query.startTime(), false);
        LocalDateTime timeEnd = parseTime(query.endTime(), true);
        wrapper.ge(timeStart != null, SysLoginLog::getLoginTime, timeStart)
            .lt(timeEnd != null, SysLoginLog::getLoginTime, timeEnd);
        Page<SysLoginLog> page = sysLoginLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<LoginLogVO> list = page.getRecords().stream().map(loginLogConverter::toVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, list);
    }

    /**
     * 入参 DTO → Mapper 查询参数（时间字符串解析为左闭右开的 LocalDateTime）。
     */
    private AuditLogQueryParam buildAuditLogQueryParam(AuditLogQueryDTO query) {
        AuditLogQueryParam param = new AuditLogQueryParam();
        param.setBizCode(query.bizCode());
        param.setModule(query.module());
        param.setBizType(query.bizType());
        param.setOperateType(query.operateType());
        param.setOperateBy(query.operateBy());
        param.setResultStatus(query.resultStatus());
        param.setTimeStart(parseTime(query.startTime(), false));
        param.setTimeEnd(parseTime(query.endTime(), true));
        return param;
    }

    /**
     * 时间解析：yyyy-MM-dd（起止各按全天边界展开）或 yyyy-MM-dd HH:mm:ss；终点为右开区间。
     */
    private LocalDateTime parseTime(String text, boolean endExclusive) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            if (text.length() == 10) {
                LocalDate date = LocalDate.parse(text, DATE_FORMATTER);
                return endExclusive ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();
            }
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "时间参数格式非法，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    /**
     * 变更字段清单：对比 before_json/after_json 的键值差异（新增/删除/值变化均视为变更字段）。
     */
    private List<String> resolveDiffFields(String beforeJson, String afterJson) {
        JsonNode before = parseJsonObject(beforeJson);
        JsonNode after = parseJsonObject(afterJson);
        if (before == null && after == null) {
            return List.of();
        }
        TreeSet<String> diff = new TreeSet<>();
        if (before != null) {
            collectDiff(before, after, diff);
        }
        if (after != null) {
            collectDiff(after, before, diff);
        }
        return new ArrayList<>(diff);
    }

    /**
     * 将 source 中与 target 有差异的字段名加入结果集（null 值与缺失字段视为等价）。
     */
    private void collectDiff(JsonNode source, JsonNode target, TreeSet<String> diff) {
        for (Map.Entry<String, JsonNode> entry : source.properties()) {
            String name = entry.getKey();
            JsonNode targetValue = target == null ? null : target.get(name);
            if (targetValue == null || targetValue.isNull()) {
                if (entry.getValue() != null && !entry.getValue().isNull()) {
                    diff.add(name);
                }
            } else if (!Objects.equals(entry.getValue(), targetValue)) {
                diff.add(name);
            }
        }
    }

    /**
     * 解析 JSON 对象，非法或非对象内容返回 null（日志数据不阻断详情查看，仅告警）。
     */
    private JsonNode parseJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            return node != null && node.isObject() ? node : null;
        } catch (JacksonException e) {
            log.warn("审计日志变更 JSON 解析失败，diffFields 跳过：{}", e.getMessage());
            return null;
        }
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? PageConstants.DEFAULT_PAGE_NUM : pageNum;
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize < 1 || pageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "每页条数须在 1-200 之间");
        }
        return pageSize;
    }
}
