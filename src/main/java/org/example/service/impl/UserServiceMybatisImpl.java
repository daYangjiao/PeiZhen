package org.example.service.impl;

import org.example.dao.UserMapper;
import org.example.model.User;
import org.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceMybatisImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceMybatisImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Override
    public User findById(Integer id) {
        return userMapper.findById(id);
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Transactional
    @Override
    public int register(User user) {
        if (userMapper.findByUsername(user.getUsername()) != null) {
            logger.warn("用户名 {} 已存在，拒绝注册", user.getUsername());
            throw new IllegalArgumentException("用户名已存在");
        }
        if (user.getUserType() == null) {
            user.setUserType(0); // 默认为普通用户
        }
        // 在这里可以添加密码加密逻辑
        // user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.save(user);
        logger.info("用户 {} 注册成功，ID: {}", user.getUsername(), user.getId());
        return user.getId();
    }

    @Override
    public int update(User user) {
        return userMapper.update(user);
    }

    @Override
    public int delete(Integer id) {
        return userMapper.delete(id);
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            logger.warn("登录失败：用户 {} 不存在", username);
            return null;
        }
        // 在这里可以添加密码匹配逻辑
        // if (passwordEncoder.matches(password, user.getPassword())) {
        if (user.getPassword().equals(password)) {
            logger.info("用户 {} 登录成功", username);
            return user;
        } else {
            logger.warn("登录失败：用户 {} 密码错误", username);
            return null;
        }
    }
    
    @Override
    public User getUserById(Integer userId) {
        return findById(userId);
    }
}