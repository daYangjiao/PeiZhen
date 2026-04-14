package org.example.service;

import org.example.model.request.AiAppointmentReplyRequest;
import org.example.model.request.AiAppointmentSessionRequest;
import org.example.model.request.AiAttendantMatchRequest;
import org.example.model.response.AiAppointmentSessionResponse;
import org.example.model.response.AiAttendantMatchResponse;

public interface AiAppointmentService {

    AiAppointmentSessionResponse createSession(Integer userId, AiAppointmentSessionRequest request);

    AiAppointmentSessionResponse getSession(String sessionId);

    AiAppointmentSessionResponse replySession(String sessionId, AiAppointmentReplyRequest request);

    AiAppointmentSessionResponse startMatch(String sessionId);

    AiAttendantMatchResponse matchAttendants(AiAttendantMatchRequest request);
}
