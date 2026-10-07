package me.north30.erp.system.user.converter;

import java.time.LocalDateTime;
import java.util.List;

import me.north30.erp.common.util.DesensitizeUtil;
import me.north30.erp.system.common.util.DateTimeFormatUtil;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.user.dto.UserCreateDTO;
import me.north30.erp.system.user.dto.UserUpdateDTO;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.vo.UserDetailVO;
import me.north30.erp.system.user.vo.UserVO;
import me.north30.erp.system.role.entity.SysUserRole;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * 用户域对象转换器（MapStruct 编译期生成，全项目唯一转换方案）。
 * <p>与 MyBatis 的 {@code org.apache.ibatis.annotations.Mapper} 无关，勿混淆；
 * MapperScan 只扫描 {@code **.mapper} 包，不会加载本接口。</p>
 * <p>ignoreByDefault + 显式声明映射字段：目标字段未声明映射即不拷贝，杜绝静默丢字段。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserConverter {

    /**
     * 新增用户 DTO → 实体。
     * <p>口令（BCrypt 加密）、仓库 ID 序列化、性别/状态缺省值、isAdmin、口令修改时间
     * 等敏感与派生字段由 Service 显式处理，不在此映射。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userCode")
    @Mapping(target = "username")
    @Mapping(target = "realName")
    @Mapping(target = "deptId")
    @Mapping(target = "phone")
    @Mapping(target = "email")
    @Mapping(target = "gender")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    SysUser toEntity(UserCreateDTO dto);

    /**
     * 修改用户 DTO → 部分更新实体（携带乐观锁 version）。
     * <p>仓库 ID 序列化与 id/updateTime 设置由 Service 处理。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "realName")
    @Mapping(target = "deptId")
    @Mapping(target = "phone")
    @Mapping(target = "email")
    @Mapping(target = "gender")
    @Mapping(target = "status")
    @Mapping(target = "remark")
    @Mapping(target = "version")
    SysUser toEntity(UserUpdateDTO dto);

    /**
     * 组装用户-角色关联实体（全删全插场景逐条构建）。
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId")
    @Mapping(target = "roleId")
    SysUserRole toUserRole(Long userId, Long roleId);

    /**
     * 实体 → 详情 VO（多源参数：用户实体 + 所属部门 + 角色派生集合）。
     * <p>手机号脱敏（maskPhone）与时间字符串化（formatTime）由本类 @Named 方法委托公共 Util；
     * dept 可为 null（用户未挂部门），MapStruct 对 null 源参数输出 null 目标字段。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "userCode", source = "user.userCode")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "realName", source = "user.realName")
    @Mapping(target = "phone", source = "user.phone", qualifiedByName = "maskPhone")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "deptId", source = "user.deptId")
    @Mapping(target = "deptName", source = "dept.deptName")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "roleIds", source = "roleIds")
    @Mapping(target = "warehouseIds", source = "warehouseIds")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "isAdmin", source = "user.isAdmin")
    @Mapping(target = "gender", source = "user.gender")
    @Mapping(target = "remark", source = "user.remark")
    @Mapping(target = "loginFailCount", source = "user.loginFailCount")
    @Mapping(target = "lockUntil", source = "user.lockUntil", qualifiedByName = "formatTime")
    @Mapping(target = "passwordUpdateTime", source = "user.passwordUpdateTime", qualifiedByName = "formatTime")
    @Mapping(target = "lastLoginTime", source = "user.lastLoginTime", qualifiedByName = "formatTime")
    @Mapping(target = "createTime", source = "user.createTime", qualifiedByName = "formatTime")
    UserDetailVO toDetailVO(SysUser user, SysDept dept,
                            List<String> roles, List<Long> roleIds, List<Long> warehouseIds);

    /**
     * 实体 → 列表项 VO（多源参数：用户实体 + 部门名称 + 角色编码集合）。
     * <p>手机号脱敏与时间字符串化复用 {@link #maskPhone}/{@link #formatTime}；
     * deptName/roles 由 Service 批量查询后传入（避免 N+1），可为 null/空集合。</p>
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "userCode", source = "user.userCode")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "realName", source = "user.realName")
    @Mapping(target = "phone", source = "user.phone", qualifiedByName = "maskPhone")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "deptId", source = "user.deptId")
    @Mapping(target = "deptName", source = "deptName")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "isAdmin", source = "user.isAdmin")
    @Mapping(target = "lastLoginTime", source = "user.lastLoginTime", qualifiedByName = "formatTime")
    @Mapping(target = "createTime", source = "user.createTime", qualifiedByName = "formatTime")
    UserVO toUserVO(SysUser user, String deptName, List<String> roles);

    /**
     * 手机号脱敏（委托 DesensitizeUtil，消除各处手写脱敏调用）。
     */
    @Named("maskPhone")
    default String maskPhone(String phone) {
        return DesensitizeUtil.maskPhone(phone);
    }

    /**
     * 时间 → "yyyy-MM-dd HH:mm:ss" 字符串（委托 DateTimeFormatUtil，null 安全）。
     */
    @Named("formatTime")
    default String formatTime(LocalDateTime time) {
        return DateTimeFormatUtil.format(time);
    }
}
