package org.example.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.example.model.MatchedAttendantVO;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel(description = "AI陪诊师匹配结果")
public class AiAttendantMatchResponse {

    @ApiModelProperty(value = "AI预约会话ID")
    private String sessionId;

    @ApiModelProperty(value = "预约编号")
    private String appointmentNo;

    @ApiModelProperty(value = "是否为降级结果")
    private Boolean degraded;

    @ApiModelProperty(value = "推荐列表")
    private List<MatchedAttendantVO> matchedList = new ArrayList<>();
}
