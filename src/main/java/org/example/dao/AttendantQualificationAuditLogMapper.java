package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.AttendantQualificationAuditLog;

import java.util.List;

@Mapper
public interface AttendantQualificationAuditLogMapper {

    int insert(AttendantQualificationAuditLog log);

    List<AttendantQualificationAuditLog> findLatestByUserId(@Param("userId") Integer userId,
                                                            @Param("limit") Integer limit);
}
