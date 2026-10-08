package me.north30.erp.system.role.strategy;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色装配策略：ID 列表序列化/解析等纯函数（数据范围 deptIds/userIds 逗号串存取）。
 */
@Component
public class RoleAssembleStrategy {

    /**
     * ID 列表 → 逗号串（空列表返回 null）。
     */
    public String joinIds(List<Long> ids) {
        if (isEmpty(ids)) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * 逗号串 → ID 列表（空串返回空列表）。
     */
    public List<Long> parseIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String id : ids.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                result.add(Long.valueOf(trimmed));
            }
        }
        return result;
    }

    /**
     * 判断 ID 列表为空。
     */
    public boolean isEmpty(List<Long> ids) {
        return ids == null || ids.isEmpty();
    }
}
