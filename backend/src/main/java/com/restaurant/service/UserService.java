package com.restaurant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.dto.LoginDTO;
import com.restaurant.dto.RegisterDTO;
import com.restaurant.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    User register(RegisterDTO dto);

    /**
     * 用户登录，返回 JWT token
     */
    String login(LoginDTO dto);

    /**
     * 根据 ID 查询用户
     */
    User getById(Long id);

    /**
     * 修改用户信息
     */
    void updateProfile(User user);

    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
