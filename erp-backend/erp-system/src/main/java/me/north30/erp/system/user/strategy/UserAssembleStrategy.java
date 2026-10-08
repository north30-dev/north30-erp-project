package me.north30.erp.system.user.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.common.util.DesensitizeUtil;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.role.mapper.SysRoleMapper;
import me.north30.erp.system.role.mapper.SysUserRoleMapper;
import me.north30.erp.system.user.converter.UserConverter;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.vo.UserVO;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户装配策略：VO 批量装配与 CSV 导出。
 * <p>IO 类策略（@Component），组织名/角色编码取数依赖 sys_dept/sys_user_role/sys_role；
 * 全程一次 IN 查询后内存关联，避免循环查库（N+1）。</p>
 */
@Component
@RequiredArgsConstructor
public class UserAssembleStrategy {

    /** CSV 导出 UTF-8 BOM（Excel 兼容） */
    private static final String CSV_BOM = "\uFEFF";

    private final SysDeptMapper sysDeptMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final UserConverter userConverter;

    /**
     * 批量装配用户列表 VO：组织名称、角色编码一次批量取数后内存关联。
     *
     * @param users 用户实体列表
     * @return 用户 VO 列表
     */
    public List<UserVO> toUserVOs(List<SysUser> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        Map<Long, String> deptNames = loadDeptNames(users);
        Map<Long, List<String>> roleCodes = loadRoleCodesByUserIds(
            users.stream().map(SysUser::getId).toList());
        return users.stream()
            .map(user -> userConverter.toUserVO(user,
                deptNames.get(user.getDeptId()),
                roleCodes.getOrDefault(user.getId(), List.of())))
            .toList();
    }

    /**
     * 按角色 ID 集合批量加载角色编码映射（getDetail 详情装配用）。
     */
    public Map<Long, String> loadRoleCodeMap(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        return sysRoleMapper.selectByIds(roleIds).stream()
            .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleCode));
    }

    /**
     * 导出用户列表为 CSV 字节（UTF-8 BOM + 表头 + 逐行转义拼装）。
     *
     * @param users 用户实体列表
     * @return CSV 文件内容字节
     */
    public byte[] toCsvBytes(List<SysUser> users) {
        Map<Long, String> deptNames = loadDeptNames(users);
        Map<Long, List<String>> roleCodes = loadRoleCodesByUserIds(
            users.stream().map(SysUser::getId).toList());
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_BOM);
        csv.append("用户ID,用户编号,用户名,姓名,联系电话,邮箱,所属组织,角色,状态,是否管理员,最后登录时间,创建时间").append("\r\n");
        for (SysUser user : users) {
            csv.append(escape(String.valueOf(user.getId()))).append(',')
                .append(escape(user.getUserCode())).append(',')
                .append(escape(user.getUsername())).append(',')
                .append(escape(user.getRealName())).append(',')
                .append(escape(DesensitizeUtil.maskPhone(user.getPhone()))).append(',')
                .append(escape(user.getEmail())).append(',')
                .append(escape(deptNames.get(user.getDeptId()))).append(',')
                .append(escape(String.join(";", roleCodes.getOrDefault(user.getId(), List.of())))).append(',')
                .append(escape(user.getStatus() != null && user.getStatus() == 1 ? "启用" : "停用")).append(',')
                .append(escape(user.isAdmin() ? "是" : "否")).append(',')
                .append(escape(DateTimeFormatUtil.format(user.getLastLoginTime()))).append(',')
                .append(escape(DateTimeFormatUtil.format(user.getCreateTime())))
                .append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 批量加载组织名称（一次 IN 查询，避免循环查库）。
     */
    private Map<Long, String> loadDeptNames(List<SysUser> users) {
        Set<Long> deptIds = users.stream()
            .map(SysUser::getDeptId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            // 用 HashMap 而非 Map.of()：用户 dept_id 可为 null，不可变 Map.get(null) 会抛 NPE
            return new HashMap<>();
        }
        return sysDeptMapper.selectByIds(deptIds).stream()
            .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
    }

    /**
     * 批量加载用户角色编码映射（user_role + role 各一次 IN 查询，避免循环查库）。
     */
    private Map<Long, List<String>> loadRoleCodesByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
        if (userRoles.isEmpty()) {
            return Map.of();
        }
        Set<Long> roleIds = userRoles.stream()
            .map(SysUserRole::getRoleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> roleCodeMap = loadRoleCodeMap(roleIds);
        return userRoles.stream()
            .collect(Collectors.groupingBy(SysUserRole::getUserId,
                Collectors.mapping(ur -> roleCodeMap.get(ur.getRoleId()),
                    Collectors.filtering(Objects::nonNull, Collectors.toList()))));
    }

    /**
     * CSV 字段转义：包含逗号/引号/换行时加双引号包裹并转义内部引号。
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\r") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
