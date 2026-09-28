package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.ChangePasswordDTO;
import me.north30.erp.system.core.dto.LoginDTO;
import me.north30.erp.system.core.dto.RefreshTokenDTO;
import me.north30.erp.system.core.service.AuthService;
import me.north30.erp.system.core.vo.CaptchaVO;
import me.north30.erp.system.core.vo.ChangePasswordVO;
import me.north30.erp.system.core.vo.CurrentUserVO;
import me.north30.erp.system.core.vo.LoginVO;
import me.north30.erp.system.core.vo.LogoutVO;
import me.north30.erp.system.core.vo.MenuTreeVO;
import me.north30.erp.system.core.vo.RefreshTokenVO;
import me.north30.erp.system.core.vo.UserPermsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证授权接口（/api/auth，接口文档第四章 8 个接口）。
 * <p>captcha/login/refresh 免认证（SecurityConfig 白名单），其余需登录态。</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 4.1 获取图形验证码（免认证）。
     */
    @PostMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.success(authService.createCaptcha());
    }

    /**
     * 4.2 用户登录（免认证）。
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 4.3 刷新令牌（免认证，须携带有效 refresh token）。
     */
    @PostMapping("/refresh")
    public Result<RefreshTokenVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.success(authService.refreshToken(dto));
    }

    /**
     * 4.4 用户登出（登录态即可）。
     */
    @PostMapping("/logout")
    public Result<LogoutVO> logout() {
        return Result.success(authService.logout());
    }

    /**
     * 4.5 获取当前用户信息。
     */
    @GetMapping("/me")
    public Result<CurrentUserVO> me() {
        return Result.success(authService.currentUser());
    }

    /**
     * 4.6 获取当前用户菜单树。
     */
    @GetMapping("/menus")
    public Result<List<MenuTreeVO>> menus() {
        return Result.success(authService.currentUserMenus());
    }

    /**
     * 4.7 获取当前用户权限点集合。
     */
    @GetMapping("/perms")
    public Result<UserPermsVO> perms() {
        return Result.success(authService.currentUserPerms());
    }

    /**
     * 4.8 修改本人密码。
     */
    @PutMapping("/password")
    public Result<ChangePasswordVO> password(@Valid @RequestBody ChangePasswordDTO dto) {
        return Result.success(authService.changePassword(dto));
    }
}
