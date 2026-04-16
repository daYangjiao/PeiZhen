package org.example.dao;

import org.example.model.AiMedicalQa;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiMedicalQaMapper {

    @Insert("INSERT INTO ai_medical_qa(user_id, conversation_id, question, qa_status, deleted, thinking_process) " +
            "VALUES(#{userId}, #{conversationId}, #{question}, #{qaStatus}, 0, #{thinkingProcess})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiMedicalQa aiMedicalQa);

    @Update("UPDATE ai_medical_qa SET answer = #{answer}, qa_status = #{qaStatus}, thinking_process = #{thinkingProcess} WHERE id = #{id}")
    int updateAnswerAndStatus(AiMedicalQa aiMedicalQa);

    @Select("SELECT id, user_id as userId, conversation_id as conversationId, question, answer, qa_status as qaStatus, " +
            "thinking_process as thinkingProcess, create_time as createTime, update_time as updateTime, deleted " +
            "FROM ai_medical_qa WHERE user_id = #{userId} AND conversation_id = #{conversationId} AND deleted = 0 ORDER BY create_time ASC, id ASC")
    List<AiMedicalQa> selectByUserIdAndConversationId(@Param("userId") Integer userId, @Param("conversationId") String conversationId);

    @Select("SELECT id, user_id as userId, conversation_id as conversationId, question, answer, qa_status as qaStatus, " +
            "thinking_process as thinkingProcess, create_time as createTime, update_time as updateTime, deleted " +
            "FROM ai_medical_qa WHERE conversation_id = #{conversationId} AND deleted = 0 ORDER BY create_time ASC, id ASC")
    List<AiMedicalQa> selectByConversationId(@Param("conversationId") String conversationId);

    @Update("UPDATE ai_medical_qa SET thinking_process = #{thinkingProcess} WHERE id = #{id}")
    void updateThinkingProcess(AiMedicalQa qaRecord);

    @Select("SELECT id, user_id as userId, conversation_id as conversationId, question, answer, qa_status as qaStatus, " +
            "thinking_process as thinkingProcess, create_time as createTime, update_time as updateTime, deleted " +
            "FROM ai_medical_qa WHERE id = #{id}")
    AiMedicalQa selectById(Long id);

    @Select("SELECT id, user_id as userId, conversation_id as conversationId, question, answer, qa_status as qaStatus, " +
            "thinking_process as thinkingProcess, create_time as createTime, update_time as updateTime, deleted " +
            "FROM ai_medical_qa WHERE id = #{id} AND user_id = #{userId} AND deleted = 0")
    AiMedicalQa selectByIdAndUserId(@Param("id") Long id, @Param("userId") Integer userId);

    @Select("SELECT conversation_id FROM ai_medical_qa " +
            "WHERE user_id = #{userId} AND deleted = 0 AND conversation_id IS NOT NULL AND conversation_id <> '' " +
            "ORDER BY create_time DESC, id DESC LIMIT 1")
    String selectLatestConversationIdByUserId(@Param("userId") Integer userId);

    @Select("SELECT COUNT(1) FROM ai_medical_qa WHERE user_id = #{userId} AND conversation_id = #{conversationId} AND deleted = 0")
    int countByUserIdAndConversationId(@Param("userId") Integer userId, @Param("conversationId") String conversationId);
}
