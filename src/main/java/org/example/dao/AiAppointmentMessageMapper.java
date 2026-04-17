package org.example.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.example.model.AiAppointmentMessageRecord;

import java.util.List;

@Mapper
public interface AiAppointmentMessageMapper {

    @Insert("""
            INSERT INTO ai_appointment_message
            (session_id, role, content, field_patch_json, deleted)
            VALUES
            (#{sessionId}, #{role}, #{content}, #{fieldPatchJson}, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiAppointmentMessageRecord record);

    @Select("""
            SELECT id, session_id AS sessionId, role, content, field_patch_json AS fieldPatchJson,
                   deleted, create_time AS createTime, update_time AS updateTime
            FROM ai_appointment_message
            WHERE session_id = #{sessionId} AND deleted = 0
            ORDER BY create_time ASC, id ASC
            """)
    List<AiAppointmentMessageRecord> selectBySessionId(String sessionId);
}
