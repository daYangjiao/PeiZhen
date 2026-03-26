package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.User;

import java.util.List;

@Mapper
public interface UserMapper {

    int save(User user);

    int update(User user);

    int delete(Integer id);

    User findById(Integer id);

    User findByPhone(String phone);

    User findByOpenid(String openid);

    List<User> findAll();

    int countAdminUsers(@Param("keyword") String keyword,
                        @Param("userType") Integer userType,
                        @Param("status") Integer status);

    List<User> findAdminUsers(@Param("keyword") String keyword,
                              @Param("userType") Integer userType,
                              @Param("status") Integer status,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    long countByUserType(@Param("userType") Integer userType);

    long countByUserTypeAndStatus(@Param("userType") Integer userType,
                                  @Param("status") Integer status);
}
