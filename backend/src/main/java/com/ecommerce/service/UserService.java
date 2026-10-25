package com.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.common.util.JwtUtil;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.LoginResponse;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.UserVO;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserVO register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() == null ? request.getUsername() : request.getNickname());
        user.setStatus(1);
        user.setRole(0);
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        return toVO(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.USER_DISABLED);
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return LoginResponse.builder().token(token).user(toVO(user)).build();
    }

    public UserVO getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toVO(user);
    }

    /**
     * 校验当前用户是否为管理员
     */
    public void requireAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
    }

    public UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        return vo;
    }
}