package org.example.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.model.AdminTaskClaim;

import java.util.Date;

@Mapper
public interface AdminTaskClaimMapper {

    AdminTaskClaim findByTask(@Param("taskType") String taskType, @Param("targetId") Integer targetId);

    void tryClaim(@Param("claim") AdminTaskClaim claim, @Param("now") Date now);

    int countActiveByOperator(@Param("operatorId") Integer operatorId, @Param("now") Date now);

    int releaseByOwner(@Param("taskType") String taskType,
                       @Param("targetId") Integer targetId,
                       @Param("operatorId") Integer operatorId);

    int deleteByTask(@Param("taskType") String taskType, @Param("targetId") Integer targetId);
}
