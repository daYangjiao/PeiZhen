package org.example.model;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * AI导诊-步骤3：创建订单请求（选择陪诊师+确认订单）
 */
@Data
public class GuideOrderRequest {
    // 选中的陪诊师ID（必填，Integer类型适配原有Attendant的id）
    @NotNull(message = "陪诊师ID不能为空")
    private String attendantId;

    // 补充备注（对应页面3的“补充备注”输入框，选填）
    private String remark;


    // 服务类型（固定多选选项：就诊陪同、检查引导、取药指导、病情记录、康复建议、饮食指导）
    // 前端可选/多选，后端默认赋值全选
    private List<String> serviceTypes = new ArrayList<String>() {{
        add("就诊陪同");
        add("检查引导");
        add("取药指导");
        add("病情记录");
        add("康复建议");
        add("饮食指导");
    }};
}