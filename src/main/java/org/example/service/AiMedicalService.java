package org.example.service;

import org.example.model.MedicalQaRequest;
import org.example.model.MedicalQaResponse;

import java.util.List;

public interface AiMedicalService {
    MedicalQaResponse submitQuestion(Integer userId, MedicalQaRequest request);

    MedicalQaResponse getRecord(Integer userId, Long recordId);

    List<MedicalQaResponse> getConversation(Integer userId, String conversationId);

    List<MedicalQaResponse> getLatestConversation(Integer userId);
}
