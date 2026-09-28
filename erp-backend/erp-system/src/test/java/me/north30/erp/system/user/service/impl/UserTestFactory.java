package me.north30.erp.system.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.user.dto.UserCreateDTO;
import me.north30.erp.system.user.entity.SysUser;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户域单元测试数据工厂：集中构建实体、DTO 与 MP 表信息缓存，供同域测试类共用。
 */
public final class UserTestFactory {

    /** 通用创建时间 */
    public static final LocalDateTime TIME_CREATE = LocalDateTime.of(2026, 1, 1, 8, 0, 0);

    /** 通用最后登录时间 */
    public static final LocalDateTime TIME_LOGIN = LocalDateTime.of(2026, 1, 2, 9, 30, 0);

    private UserTestFactory() {
    }

    /**
     * 初始化测试涉及实体的 MP TableInfo 缓存：Lambda 列解析依赖该缓存，
     * 纯 Mockito 单测环境无 MyBatis 启动流程，必须手动初始化。
     */
    public static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, SysDept.class);
    }

    /** 默认启用、非管理员用户：id=10，组织 3，手机号 13812345678，仓库串含空段（用于解析边界） */
    public static SysUser sysUser() {
        SysUser user = new SysUser();
        user.setId(10L);
        user.setUserCode("E001");
        user.setUsername("zhangsan");
        user.setPassword("encodedPwd");
        user.setRealName("张三");
        user.setDeptId(3L);
        user.setWarehouseIds("1, ,2");
        user.setPhone("13812345678");
        user.setEmail("zs@north30.com");
        user.setGender(1);
        user.setStatus(1);
        user.setIsAdmin(0);
        user.setLoginFailCount(1);
        user.setLastLoginTime(TIME_LOGIN);
        user.setCreateTime(TIME_CREATE);
        user.setRemark("测试用户");
        return user;
    }

    /** 内置管理员用户（is_admin=1） */
    public static SysUser adminUser() {
        SysUser user = sysUser();
        user.setIsAdmin(1);
        return user;
    }

    /** 默认组织：id=3，祖先路径 0,1 */
    public static SysDept sysDept() {
        SysDept dept = new SysDept();
        dept.setId(3L);
        dept.setDeptName("生产部");
        dept.setParentId(1L);
        dept.setAncestors("0,1");
        return dept;
    }

    /** 指定 ID 与编码的角色 */
    public static SysRole sysRole(Long id, String roleCode) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setRoleCode(roleCode);
        role.setRoleName("角色" + id);
        return role;
    }

    /** 用户-角色关联 */
    public static SysUserRole sysUserRole(Long userId, Long roleId) {
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    /** 合法新增用户请求：密码满足复杂度，角色 [1,2]，仓库 [1,2]，性别/状态留空走默认值 */
    public static UserCreateDTO validUserCreateDTO() {
        return new UserCreateDTO("E001", "zhangsan", "Passw0rd!", "张三", 3L,
            List.of(1L, 2L), "13812345678", "zs@north30.com", null, null,
            List.of(1L, 2L), null);
    }
}
