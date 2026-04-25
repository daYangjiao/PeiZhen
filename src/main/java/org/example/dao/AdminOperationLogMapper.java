package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.AdminOperationLog;

import java.util.List;

@Mapper
public interface AdminOperationLogMapper {

    int insert(AdminOperationLog log);

    int countLogs(@Param("module") String module,
                  @Param("action") String action,
                  @Param("operatorRole") String operatorRole,
                  @Param("keyword") String keyword,
                  @Param("startTime") String startTime,
                  @Param("endTime") String endTime);

    List<AdminOperationLog> findLogs(@Param("module") String module,
                                     @Param("action") String action,
                                     @Param("operatorRole") String operatorRole,
                                     @Param("keyword") String keyword,
                                     @Param("startTime") String startTime,
                                     @Param("endTime") String endTime,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);
}
