package me.north30.erp.system.user.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.user.dto.UserQueryDTO;
import me.north30.erp.system.user.entity.SysUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 用户查询策略：列表查询条件构造、组织树展开、分页参数归一。
 * <p>IO 类策略（@Component），组织展开需查 sys_dept；分页归一为防御二层，
 * 供绕过 HTTP 直调 Service 时兜底（pageNum/pageSize 缺省与上限截断）。</p>
 */
@Component
@RequiredArgsConstructor
public class UserQueryStrategy {

    private final SysDeptMapper sysDeptMapper;

    /**
     * 构建列表查询条件：username/realName 模糊、userCode 精确、status 精确、deptId 含下级组织。
     *
     * @param query 查询参数
     * @return LambdaQueryWrapper<SysUser> 查询条件包装器（排序由调用方追加）
     */
    public LambdaQueryWrapper<SysUser> buildListWrapper(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
            .like(StringUtils.hasText(query.username()), SysUser::getUsername, query.username())
            .like(StringUtils.hasText(query.realName()), SysUser::getRealName, query.realName())
            .eq(StringUtils.hasText(query.userCode()), SysUser::getUserCode, query.userCode())
            .eq(query.status() != null, SysUser::getStatus, query.status());
        if (query.deptId() != null) {
            wrapper.in(SysUser::getDeptId, resolveDeptIdsWithDescendants(query.deptId()));
        }
        return wrapper;
    }

    /**
     * 分页查询参数解析：默认第 1 页，每页 10 条（接口文档 1.4）。
     * 
     * @param query 查询参数
     * @return 分页页码（默认第 1 页）
     */
    public int resolvePageNum(UserQueryDTO query) {
        if (query.pageNum() == null || query.pageNum() < 1) {
            return PageConstants.DEFAULT_PAGE_NUM;
        }
        return query.pageNum();
    }

    /**
     * 每页条数上限按 PageConstants.MAX_PAGE_SIZE 截断（接口文档 1.4）。
     * 
     * @param query 查询参数
     * @return 每页条数（默认 10 条）
     */
    public int resolvePageSize(UserQueryDTO query) {
        if (query.pageSize() == null || query.pageSize() < 1) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(query.pageSize(), PageConstants.MAX_PAGE_SIZE);
    }

    /**
     * 解析组织及下级组织 ID 集合：组织规模小（≤百级），一次全量查询后按 ancestors 路径内存过滤，
     * 避免 LIKE 拼接注入面与循环查库。
     * 
     * @param deptId 组织 ID
     * @return 组织 ID 映射下级组织 ID 集合的映射
     * @return 组织 ID 映射下级组织 ID 集合的映射
     */
    private List<Long> resolveDeptIdsWithDescendants(Long deptId) {
        List<SysDept> depts = sysDeptMapper.selectList(
            new LambdaQueryWrapper<SysDept>().select(SysDept::getId, SysDept::getAncestors));
        List<Long> deptIds = new ArrayList<>();
        deptIds.add(deptId);
        for (SysDept dept : depts) {
            if (dept.getId().equals(deptId)) {
                continue;
            }
            if (containsAncestor(dept.getAncestors(), deptId)) {
                deptIds.add(dept.getId());
            }
        }
        return deptIds;
    }

    /**
     * 判断 ancestors 祖级路径（如 0,1,5）是否包含指定组织 ID。
     * 
     * @param ancestors 祖级路径（逗号分隔）
     * @param deptId 组织 ID
     * @return 是否包含指定组织 ID
     */
    private boolean containsAncestor(String ancestors, Long deptId) {
        if (ancestors == null || ancestors.isBlank()) {
            return false;
        }
        return Arrays.stream(ancestors.split(","))
            .anyMatch(part -> part.trim().equals(String.valueOf(deptId)));
    }
}
