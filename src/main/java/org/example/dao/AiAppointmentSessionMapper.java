package org.example.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.model.AiAppointmentSessionRecord;

@Mapper
public interface AiAppointmentSessionMapper {

    @Insert("""
            INSERT INTO ai_appointment_session
            (session_id, user_id, status, processing_phase, thinking_process, assistant_reply, assistant_intent,
             need_more_info, missing_fields_json, question_type, question_key, follow_up_type, time_proposal_json, options_json,
             can_match, ready_for_confirm, follow_up_round, raw_demand_text, structured_demand_json,
             field_patch_json, confirm_summary_json, matched_list_json, appointment_no, degraded, deleted)
            VALUES
            (#{sessionId}, #{userId}, #{status}, #{processingPhase}, #{thinkingProcess}, #{assistantReply}, #{assistantIntent},
             #{needMoreInfo}, #{missingFieldsJson}, #{questionType}, #{questionKey}, #{followUpType}, #{timeProposalJson}, #{optionsJson},
             #{canMatch}, #{readyForConfirm}, #{followUpRound}, #{rawDemandText}, #{structuredDemandJson},
             #{fieldPatchJson}, #{confirmSummaryJson}, #{matchedListJson}, #{appointmentNo}, #{degraded}, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiAppointmentSessionRecord record);

    @Select("""
            SELECT id, session_id AS sessionId, user_id AS userId, status, processing_phase AS processingPhase,
                   thinking_process AS thinkingProcess, assistant_reply AS assistantReply, assistant_intent AS assistantIntent,
                   need_more_info AS needMoreInfo, missing_fields_json AS missingFieldsJson,
                   question_type AS questionType, question_key AS questionKey, follow_up_type AS followUpType,
                   time_proposal_json AS timeProposalJson, options_json AS optionsJson,
                   can_match AS canMatch, ready_for_confirm AS readyForConfirm, follow_up_round AS followUpRound,
                   raw_demand_text AS rawDemandText, structured_demand_json AS structuredDemandJson,
                   field_patch_json AS fieldPatchJson, confirm_summary_json AS confirmSummaryJson,
                   matched_list_json AS matchedListJson, appointment_no AS appointmentNo,
                   degraded, deleted, create_time AS createTime, update_time AS updateTime
            FROM ai_appointment_session
            WHERE session_id = #{sessionId} AND deleted = 0
            LIMIT 1
            """)
    AiAppointmentSessionRecord selectBySessionId(String sessionId);

    @Select("""
            SELECT id, session_id AS sessionId, user_id AS userId, status, processing_phase AS processingPhase,
                   thinking_process AS thinkingProcess, assistant_reply AS assistantReply, assistant_intent AS assistantIntent,
                   need_more_info AS needMoreInfo, missing_fields_json AS missingFieldsJson,
                   question_type AS questionType, question_key AS questionKey, follow_up_type AS followUpType,
                   time_proposal_json AS timeProposalJson, options_json AS optionsJson,
                   can_match AS canMatch, ready_for_confirm AS readyForConfirm, follow_up_round AS followUpRound,
                   raw_demand_text AS rawDemandText, structured_demand_json AS structuredDemandJson,
                   field_patch_json AS fieldPatchJson, confirm_summary_json AS confirmSummaryJson,
                   matched_list_json AS matchedListJson, appointment_no AS appointmentNo,
                   degraded, deleted, create_time AS createTime, update_time AS updateTime
            FROM ai_appointment_session
            WHERE user_id = #{userId}
              AND deleted = 0
              AND status IN ('COLLECTING', 'READY', 'MATCHING')
              AND (appointment_no IS NULL OR appointment_no = '')
            ORDER BY update_time DESC, id DESC
            LIMIT 1
            """)
    AiAppointmentSessionRecord selectLatestRestorableByUserId(Integer userId);

    @Select("""
            SELECT id, session_id AS sessionId, user_id AS userId, status, processing_phase AS processingPhase,
                   thinking_process AS thinkingProcess, assistant_reply AS assistantReply, assistant_intent AS assistantIntent,
                   need_more_info AS needMoreInfo, missing_fields_json AS missingFieldsJson,
                   question_type AS questionType, question_key AS questionKey, follow_up_type AS followUpType,
                   time_proposal_json AS timeProposalJson, options_json AS optionsJson,
                   can_match AS canMatch, ready_for_confirm AS readyForConfirm, follow_up_round AS followUpRound,
                   raw_demand_text AS rawDemandText, structured_demand_json AS structuredDemandJson,
                   field_patch_json AS fieldPatchJson, confirm_summary_json AS confirmSummaryJson,
                   matched_list_json AS matchedListJson, appointment_no AS appointmentNo,
                   degraded, deleted, create_time AS createTime, update_time AS updateTime
            FROM ai_appointment_session
            WHERE user_id = #{userId}
              AND deleted = 0
            ORDER BY update_time DESC, id DESC
            LIMIT 1
            """)
    AiAppointmentSessionRecord selectLatestByUserId(Integer userId);
    @Update("""
            UPDATE ai_appointment_session
            SET status = #{status},
                processing_phase = #{processingPhase},
                thinking_process = #{thinkingProcess},
                assistant_reply = #{assistantReply},
                assistant_intent = #{assistantIntent},
                need_more_info = #{needMoreInfo},
                missing_fields_json = #{missingFieldsJson},
                question_type = #{questionType},
                question_key = #{questionKey},
                follow_up_type = #{followUpType},
                time_proposal_json = #{timeProposalJson},
                options_json = #{optionsJson},
                can_match = #{canMatch},
                ready_for_confirm = #{readyForConfirm},
                follow_up_round = #{followUpRound},
                raw_demand_text = #{rawDemandText},
                structured_demand_json = #{structuredDemandJson},
                field_patch_json = #{fieldPatchJson},
                confirm_summary_json = #{confirmSummaryJson},
                matched_list_json = #{matchedListJson},
                appointment_no = #{appointmentNo},
                degraded = #{degraded},
                update_time = NOW()
            WHERE session_id = #{sessionId} AND deleted = 0
            """)
    int updateBySessionId(AiAppointmentSessionRecord record);
}
