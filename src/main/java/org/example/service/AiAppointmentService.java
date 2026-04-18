package org.example.service;

import org.example.model.request.AiAppointmentReplyRequest;
import org.example.model.request.AiAppointmentSessionRequest;
import org.example.model.request.AiAttendantMatchRequest;
import org.example.model.response.AiAppointmentLatestOverviewResponse;
import org.example.model.response.AiAppointmentSessionResponse;
import org.example.model.response.AiAttendantMatchResponse;

public interface AiAppointmentService {

    AiAppointmentSessionResponse createSession(Integer userId, AiAppointmentSessionRequest request);

    AiAppointmentSessionResponse getSession(String sessionId);

    AiAppointmentSessionResponse getSession(Integer userId, String sessionId);

    AiAppointmentSessionResponse getLatestRestorableSession(Integer userId);

    AiAppointmentLatestOverviewResponse getLatestSessionOverview(Integer userId);

    AiAppointmentSessionResponse replySession(String sessionId, AiAppointmentReplyRequest request);

    AiAppointmentSessionResponse replySession(Integer userId, String sessionId, AiAppointmentReplyRequest request);

    AiAppointmentSessionResponse startMatch(String sessionId);

    AiAppointmentSessionResponse startMatch(Integer userId, String sessionId);

    AiAttendantMatchResponse matchAttendants(AiAttendantMatchRequest request);
}
