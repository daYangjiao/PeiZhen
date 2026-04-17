package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.example.model.AiAppointmentTimeProposal;
import org.example.model.AiAppointmentStructuredDemand;
import org.example.model.MatchedAttendantVO;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ApiModel(description = "AI预约会话响应")
public class AiAppointmentSessionResponse {

    @ApiModelProperty(value = "会话ID")
    private String sessionId;

    @ApiModelProperty(value = "业务状态", example = "COLLECTING")
    private String status;

    @ApiModelProperty(value = "处理阶段", example = "thinking")
    private String processingPhase;

    @ApiModelProperty(value = "当前处理提示", example = "正在理解您的预约需求...")
    private String thinkingProcess;

    @ApiModelProperty(value = "AI消息内容")
    private String message;

    @ApiModelProperty(value = "AI当前轮回复")
    private String assistantReply;

    @ApiModelProperty(value = "AI当前轮意图", example = "collect")
    private String assistantIntent;

    @ApiModelProperty(value = "是否仍需补充信息")
    private Boolean needMoreInfo;

    @ApiModelProperty(value = "缺失字段")
    private List<String> missingFields = new ArrayList<>();

    @ApiModelProperty(value = "问题类型", example = "serviceDate")
    private String questionType;

    @ApiModelProperty(value = "当前追问字段", example = "serviceDate")
    private String questionKey;

    @ApiModelProperty(value = "可选项")
    private List<String> options = new ArrayList<>();

    @ApiModelProperty(value = "是否可以发起匹配")
    private Boolean canMatch;

    @ApiModelProperty(value = "是否可显示确认卡")
    private Boolean readyForConfirm;

    @ApiModelProperty(value = "建议前端触发的补充方式", example = "time_picker")
    private String followUpType;

    @ApiModelProperty(value = "AI 给出的时间建议")
    private AiAppointmentTimeProposal timeProposal;

    @ApiModelProperty(value = "追问轮次")
    private Integer followUpRound;

    @ApiModelProperty(value = "结构化需求")
    private AiAppointmentStructuredDemand structuredDemand;

    @ApiModelProperty(value = "本轮提取出的字段增量")
    private AiAppointmentStructuredDemand fieldPatch;

    @ApiModelProperty(value = "确认卡摘要")
    private Map<String, Object> confirmSummary = new LinkedHashMap<>();

    @ApiModelProperty(value = "历史聊天消息")
    private List<AiAppointmentChatMessageVO> messages = new ArrayList<>();

    @ApiModelProperty(value = "匹配到的陪诊师")
    private List<MatchedAttendantVO> matchedList = new ArrayList<>();

    @ApiModelProperty(value = "预约编号")
    private String appointmentNo;

    @ApiModelProperty(value = "是否已降级")
    private Boolean degraded;
}
