package me.north30.erp.system.role.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.role.dto.RolePageQueryDTO;
import me.north30.erp.system.role.entity.SysRole;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 角色分页查询策略：分页参数防御、排序字段白名单与查询条件构造。
 */
@Component
public class RoleQueryStrategy {

    /** 默认排序字段 */
    private static final String DEFAULT_ORDER_BY = "create_time";

    /** 排序字段白名单（接口文档 1.4：orderBy 必须命中白名单，否则抛 10001） */
    private static final Set<String> ORDER_BY_WHITELIST = Set.of("create_time", "role_code", "role_sort", "role_name");

    /**
     * 构造分页对象：分页参数与排序方向校验不合法抛 10001，排序字段命中白名单后写入 Page。
     */
    public Page<SysRole> buildPage(RolePageQueryDTO query) {
        int pageNum = query.pageNum() == null ? PageConstants.DEFAULT_PAGE_NUM : query.pageNum();
        int pageSize = query.pageSize() == null ? PageConstants.DEFAULT_PAGE_SIZE : query.pageSize();
        if (pageNum < 1 || pageSize < 1 || pageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "分页参数非法：pageNum ≥ 1 且 1 ≤ pageSize ≤ " + PageConstants.MAX_PAGE_SIZE);
        }
        String orderBy = query.orderBy() == null || query.orderBy().isBlank()
            ? DEFAULT_ORDER_BY
            : query.orderBy();
        if (!ORDER_BY_WHITELIST.contains(orderBy)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "排序字段非法：" + orderBy);
        }
        boolean asc;
        if (query.orderDirection() == null || query.orderDirection().isBlank()) {
            asc = false;
        } else if ("ASC".equalsIgnoreCase(query.orderDirection())) {
            asc = true;
        } else if ("DESC".equalsIgnoreCase(query.orderDirection())) {
            asc = false;
        } else {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "排序方向仅支持 ASC/DESC");
        }
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        page.addOrder(asc ? OrderItem.asc(orderBy) : OrderItem.desc(orderBy));
        return page;
    }

    /**
     * 构造分页过滤条件（角色编码/名称模糊、状态精确）。
     */
    public LambdaQueryWrapper<SysRole> buildWrapper(RolePageQueryDTO query) {
        return new LambdaQueryWrapper<SysRole>()
            .like(hasText(query.roleCode()), SysRole::getRoleCode, query.roleCode())
            .like(hasText(query.roleName()), SysRole::getRoleName, query.roleName())
            .eq(query.status() != null, SysRole::getStatus, query.status());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
