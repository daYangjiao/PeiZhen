package org.example.dao;

import org.example.model.AiMedicalQa;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiMedicalQaMapper {

    @Insert("INSERT INTO ai_medical_qa(conversation_id, question, qa_status, deleted, thinking_process) " +
            "VALUES(#{conversationId}, #{question}, #{qaStatus}, 0, #{thinkingProcess})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiMedicalQa aiMedicalQa);

    @Update("UPDATE ai_medical_qa SET answer = #{answer}, qa_status = #{qaStatus}, thinking_process = #{thinkingProcess} WHERE id = #{id}")
    int updateAnswerAndStatus(AiMedicalQa aiMedicalQa);

    @Select("SELECT id, conversation_id as conversationId, question, answer, qa_status as qaStatus, " +
            "thinking_process as thinkingProcess, create_time as createTime, update_time as updateTime, deleted " +
            "FROM ai_medical_qa WHERE conversation_id = #{conversationId} AND deleted = 0 ORDER BY create_time ASC, id ASC")
    List<AiMedicalQa> selectByConversationId(@Param("conversationId") String conversationId);

    @Update("UPDATE ai_medical_qa SET thinking_process = #{thinkingProcess} WHERE id = #{id}")
    void updateThinkingProcess(AiMedicalQa qaRecord);

    @Select("SELECT id, conversation_id as conversationId, question, answer, qa_status as qaStatus, " +
            "thinking_process as thinkingProcess, create_time as createTime, update_time as updateTime, deleted " +
            "FROM ai_medical_qa WHERE id = #{id}")
    AiMedicalQa selectById(Long id);
}
