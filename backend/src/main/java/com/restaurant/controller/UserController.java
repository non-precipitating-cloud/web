package com.restaurant.controller;

import com.restaurant.common.Result;
import com.restaurant.dto.LoginDTO;
import com.restaurant.dto.RegisterDTO;
import com.restaurant.entity.User;
import com.restaurant.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.ok("注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        String token = userService.login(dto);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        return Result.ok("登录成功", data);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<User> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getById(userId);
        user.setPassword(null);
        return Result.ok(user);
    }

    /**
     * 修改个人信息
     */
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody User user, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        user.setId(userId);
        userService.updateProfile(user);
        return Result.ok("修改成功");
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<?> changePassword(@RequestBody Map<String, String> params,
                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.changePassword(userId, params.get("oldPassword"), params.get("newPassword"));
        return Result.ok("密码修改成功");
    }
}
