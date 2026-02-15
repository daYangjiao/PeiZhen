package org.example.dao;

import org.example.model.Attendant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Set;

@Mapper
public interface GuideAttendantMapper {
    // 原有方法：筛选可用陪诊师
    List<Attendant> selectAvailableAttendants(
            @Param("hospitalName") String hospitalName,
            @Param("serviceDate") String serviceDate,
            @Param("serviceTimeSlot") String serviceTimeSlot);

    // 原有方法：按标签筛选陪诊师
    List<Attendant> selectAttendantsByTags(@Param("tags") Set<String> tags);

    // 新增方法：根据ID查询陪诊师
    Attendant selectById(@Param("id") Integer id);
}