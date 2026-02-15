package org.example.dao;

import org.example.model.AiMedicalQa;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AiMedicalQaMapper {

    // 修正Insert语句：删除user_id字段，新增thinking_process字段初始化
    @Insert("INSERT INTO ai_medical_qa(conversation_id, question, qa_status, deleted, thinking_process) " +
            "VALUES(#{conversationId}, #{question}, #{qaStatus}, 0, #{thinkingProcess})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiMedicalQa aiMedicalQa);

    @Update("UPDATE ai_medical_qa SET answer = #{answer}, qa_status = #{qaStatus} WHERE id = #{id}")
    int updateAnswerAndStatus(AiMedicalQa aiMedicalQa);

    // （可选）如果需要按对话ID查历史，保留这个简化后的查询（去掉userId）
    @Select("SELECT * FROM ai_medical_qa WHERE conversation_id = #{conversationId} AND deleted = 0 ORDER BY create_time ASC")
    List<AiMedicalQa> selectByConversationId(@Param("conversationId") String conversationId);

    // 新增：更新思考过程（注解实现，无需XML）
    @Update("UPDATE ai_medical_qa SET thinking_process = #{thinkingProcess} WHERE id = #{id}")
    void updateThinkingProcess(AiMedicalQa qaRecord);

    // 新增：根据ID查询记录（包含thinking_process字段）
    @Select("SELECT id, conversation_id as conversationId, question, answer, qa_status as qaStatus, " +
            "thinking_process as thinkingProcess, create_time as createTime, update_time as updateTime " +
            "FROM ai_medical_qa WHERE id = #{id}")
    // 手动映射下划线字段到驼峰属性（确保字段匹配）
    @Results({
            @Result(column = "conversation_id", property = "conversationId"),
            @Result(column = "qa_status", property = "qaStatus"),
            @Result(column = "thinking_process", property = "thinkingProcess"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime")
    })
    AiMedicalQa selectById(Long id);
}