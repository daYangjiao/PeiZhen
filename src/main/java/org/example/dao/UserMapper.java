package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.model.User;

import java.util.List;

@Mapper
public interface UserMapper {

    int save(User user);

    int update(User user);

    int delete(Integer id);

    User findById(Integer id);

    User findByUsername(String username);
    
    User findByPhone(String phone);

    User findByOpenid(String openid);

    List<User> findAll();
}