package org.example.model;

import lombok.Data;

import java.util.Date;

@Data
public class AiAppointmentSessionRecord {
    private Long id;
    private String sessionId;
    private Integer userId;
    private String status;
    private String processingPhase;
    private String thinkingProcess;
    private String assistantReply;
    private String assistantIntent;
    private Integer needMoreInfo;
    private String missingFieldsJson;
    private String questionType;
    private String questionKey;
    private String followUpType;
    private String timeProposalJson;
    private String optionsJson;
    private Integer canMatch;
    private Integer readyForConfirm;
    private Integer followUpRound;
    private String rawDemandText;
    private String structuredDemandJson;
    private String fieldPatchJson;
    private String confirmSummaryJson;
    private String matchedListJson;
    private String appointmentNo;
    private Integer degraded;
    private Integer deleted;
    private Date createTime;
    private Date updateTime;
}
