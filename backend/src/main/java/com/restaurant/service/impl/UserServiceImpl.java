package com.restaurant.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.dto.LoginDTO;
import com.restaurant.dto.RegisterDTO;
import com.restaurant.entity.User;
import com.restaurant.exception.BusinessException;
import com.restaurant.mapper.UserMapper;
import com.restaurant.service.UserService;
import com.restaurant.util.JwtUtil;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User register(RegisterDTO dto) {
        // 检查账号是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (this.count(wrapper) > 0) {
            throw new BusinessException("账号已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setRole(0);     // 普通用户
        user.setStatus(1);   // 正常状态

        this.save(user);
        return user;
    }

    @Override
    public String login(LoginDTO dto) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = this.getOne(wrapper);

        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 校验密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }

        // 签发 JWT
        return JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    @Override
    public User getById(Long id) {
        User user = super.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 不返回密码
        user.setPassword(null);
        return user;
    }

    @Override
    public void updateProfile(User user) {
        // 只允许修改昵称、手机号、头像
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setNickname(user.getNickname());
        updateUser.setPhone(user.getPhone());
        updateUser.setAvatar(user.getAvatar());
        this.updateById(updateUser);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = super.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        this.updateById(updateUser);
    }
}
