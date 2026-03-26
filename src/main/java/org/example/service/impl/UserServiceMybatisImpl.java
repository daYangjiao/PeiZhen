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
    public User findByPhone(String phone) {
        return userMapper.findByPhone(phone);
    }

    @Override
    public List<User> findAll() {
        return userMapper.findAll();
    }

    @Transactional
    @Override
    public int register(User user) {
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (userMapper.findByPhone(user.getPhone()) != null) {
            logger.warn("手机号 {} 已存在，拒绝注册", user.getPhone());
            throw new IllegalArgumentException("手机号已存在");
        }
        if (user.getUserType() == null) {
            user.setUserType(0); // 默认为普通用户
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        encodePasswordIfNeeded(user);
        userMapper.save(user);
        logger.info("用户手机号 {} 注册成功，ID: {}", user.getPhone(), user.getId());
        return user.getId();
    }

    @Override
    public int update(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("用户信息不完整");
        }
        if (user != null && user.getPhone() != null && !user.getPhone().isBlank()) {
            User existing = userMapper.findByPhone(user.getPhone());
            if (existing != null && !existing.getId().equals(user.getId())) {
                throw new IllegalArgumentException("手机号已存在");
            }
        }
        if (!hasUpdatableFields(user)) {
            logger.info("用户 {} 未提供可更新字段，跳过数据库更新", user.getId());
            return 0;
        }
        encodePasswordIfNeeded(user);
        return userMapper.update(user);
    }

    @Override
    public int delete(Integer id) {
        return userMapper.delete(id);
    }

    @Override
    public User login(String phone, String password) {
        User user = userMapper.findByPhone(phone);
        if (user == null) {
            logger.warn("登录失败：手机号 {} 不存在", phone);
            return null;
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            logger.warn("登录失败：手机号 {} 已被禁用", phone);
            return null;
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            logger.warn("登录失败：手机号 {} 未设置有效密码", phone);
            return null;
        }

        if (isEncodedPassword(storedPassword)) {
            if (passwordEncoder.matches(password, storedPassword)) {
                logger.info("手机号 {} 登录成功", phone);
                return user;
            }
        } else if (storedPassword.equals(password)) {
            upgradeLegacyPassword(user, password);
            logger.info("手机号 {} 登录成功", phone);
            return user;
        }

        logger.warn("登录失败：手机号 {} 密码错误", phone);
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
        logger.info("手机号 {} 的旧版明文密码已升级为加密存储", user.getPhone());
    }

    private boolean hasUpdatableFields(User user) {
        return user.getPassword() != null
            || user.getName() != null
            || user.getPhone() != null
            || user.getSex() != null
            || user.getAge() != null
            || user.getAvatar() != null
            || user.getUserType() != null
            || user.getOpenid() != null;
    }
}
