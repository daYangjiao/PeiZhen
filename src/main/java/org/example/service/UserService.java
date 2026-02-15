package org.example.service;

import org.example.model.User;
import java.util.List;

public interface UserService {

    User findById(Integer id);

    User findByUsername(String username);

    List<User> findAll();

    int register(User user);

    int update(User user);

    int delete(Integer id);

    User login(String username, String password);
    
    /**
     * 根据ID获取用户
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Integer userId);
}