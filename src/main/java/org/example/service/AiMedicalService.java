package org.example.service;

import org.example.model.MedicalQaRequest;
import org.example.model.MedicalQaResponse;

import java.util.List;

public interface AiMedicalService {
    MedicalQaResponse submitQuestion(MedicalQaRequest request);

    MedicalQaResponse getRecord(Long recordId);

    List<MedicalQaResponse> getConversation(String conversationId);
}
