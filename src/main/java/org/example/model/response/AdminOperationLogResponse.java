package org.example.model.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminOperationLogResponse {

    private Long id;

    private Integer operatorId;

    private String operatorName;

    private String operatorPhoneMasked;

    private String operatorRole;

    private String module;

    private String action;

    private String targetType;

    private Integer targetId;

    private String targetLabel;

    private Integer fromStatus;

    private Integer toStatus;

    private String remark;

    private Date createTime;
}
