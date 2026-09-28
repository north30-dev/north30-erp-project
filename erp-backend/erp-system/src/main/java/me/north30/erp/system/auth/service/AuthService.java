package me.north30.erp.system.auth.service;

import me.north30.erp.system.auth.dto.ChangePasswordDTO;
import me.north30.erp.system.auth.dto.LoginDTO;
import me.north30.erp.system.auth.dto.RefreshTokenDTO;
import me.north30.erp.system.auth.vo.CaptchaVO;
import me.north30.erp.system.auth.vo.ChangePasswordVO;
import me.north30.erp.system.auth.vo.CurrentUserVO;
import me.north30.erp.system.auth.vo.LoginVO;
import me.north30.erp.system.auth.vo.LogoutVO;
import me.north30.erp.system.menu.vo.MenuTreeVO;
import me.north30.erp.system.auth.vo.RefreshTokenVO;
import me.north30.erp.system.auth.vo.UserPermsVO;

import java.util.List;

/**
 * 认证授权服务接口（接口文档第四章 /api/auth 8 接口）。
 */
public interface AuthService {

    /** 4.1 获取图形验证码（免认证） */
    CaptchaVO createCaptcha();

    /** 4.2 用户登录（免认证） */
    LoginVO login(LoginDTO dto);

    /** 4.3 刷新令牌（免认证，须携带有效 refresh token） */
    RefreshTokenVO refreshToken(RefreshTokenDTO dto);

    /** 4.4 用户登出（登录态即可） */
    LogoutVO logout();

    /** 4.5 获取当前用户信息 */
    CurrentUserVO currentUser();

    /** 4.6 获取当前用户菜单树 */
    List<MenuTreeVO> currentUserMenus();

    /** 4.7 获取当前用户权限点集合 */
    UserPermsVO currentUserPerms();

    /** 4.8 修改本人密码 */
    ChangePasswordVO changePassword(ChangePasswordDTO dto);
}
