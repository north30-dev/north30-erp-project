package me.north30.erp.system.user.converter;

import me.north30.erp.system.user.dto.UserCreateDTO;
import me.north30.erp.system.user.dto.UserUpdateDTO;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.vo.UserDetailVO;
import me.north30.erp.system.role.entity.SysUserRole;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
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
     * 实体 → 详情 VO。
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
    @Mapping(target = "isAdmin")
    UserDetailVO toDetailVO(SysUser user);
}
