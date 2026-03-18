package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.AttendantQualification;

@Mapper
public interface AttendantQualificationMapper {

    AttendantQualification findByUserId(@Param("userId") Integer userId);

    int insert(AttendantQualification qualification);

    int updateByUserId(AttendantQualification qualification);
}
