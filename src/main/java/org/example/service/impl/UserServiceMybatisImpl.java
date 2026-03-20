package org.example.service.impl;

import org.example.dao.UserMapper;
import org.example.model.User;
import org.example.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserServiceMybatisImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceMybatisImpl.class);
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]?\\$.{56}$");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        encodePasswordIfNeeded(user);
        userMapper.save(user);
        logger.info("用户 {} 注册成功，ID: {}", user.getUsername(), user.getId());
        return user.getId();
    }

    @Override
    public int update(User user) {
        encodePasswordIfNeeded(user);
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

        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            logger.warn("登录失败：用户 {} 未设置有效密码", username);
            return null;
        }

        if (isEncodedPassword(storedPassword)) {
            if (passwordEncoder.matches(password, storedPassword)) {
                logger.info("用户 {} 登录成功", username);
                return user;
            }
        } else if (storedPassword.equals(password)) {
            upgradeLegacyPassword(user, password);
            logger.info("用户 {} 登录成功", username);
            return user;
        }

        logger.warn("登录失败：用户 {} 密码错误", username);
        return null;
    }
    
    @Override
    public User getUserById(Integer userId) {
        return findById(userId);
    }

    private void encodePasswordIfNeeded(User user) {
        if (user == null) {
            return;
        }

        String password = user.getPassword();
        if (password == null || password.isBlank() || isEncodedPassword(password)) {
            return;
        }

        user.setPassword(passwordEncoder.encode(password));
    }

    private boolean isEncodedPassword(String password) {
        return password != null && BCRYPT_PATTERN.matcher(password).matches();
    }

    private void upgradeLegacyPassword(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        userMapper.update(user);
        logger.info("用户 {} 的旧版明文密码已升级为加密存储", user.getUsername());
    }
}
