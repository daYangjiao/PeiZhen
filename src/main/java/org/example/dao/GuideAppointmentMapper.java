package org.example.dao;

import org.example.model.GuideAppointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GuideAppointmentMapper {
    // 保存预约信息
    int insertAppointment(GuideAppointment appointment);
    
    // 新增：保存预约信息并返回自增ID
    int insertGuideAppointment(GuideAppointment appointment);

    // 根据ID查询预约
    GuideAppointment selectById(@Param("appointmentId") String appointmentId);
    
    // 根据预约编号查询预约
    GuideAppointment selectByAppointmentNo(@Param("appointmentNo") String appointmentNo);
    
    // 查询最新的预约记录
    GuideAppointment selectLatest();
}