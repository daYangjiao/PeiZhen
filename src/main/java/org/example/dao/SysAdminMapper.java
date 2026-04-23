package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.entity.SysAdmin;

import java.util.List;

@Mapper
public interface SysAdminMapper {

    SysAdmin findById(@Param("id") Integer id);

    SysAdmin findByPhone(@Param("phone") String phone);

    int countAdmins(@Param("keyword") String keyword, @Param("status") Integer status);

    int countEnabledSuperAdmins();

    List<SysAdmin> findAdmins(@Param("keyword") String keyword,
                              @Param("status") Integer status,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    int insert(SysAdmin admin);

    int update(SysAdmin admin);
}
