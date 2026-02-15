package org.example.model;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * AI导诊-步骤2：需求填写（用于匹配陪诊师）
 */
@Data
public class GuideDemandRequest {

    // 需求类型（必填：手术/检查/普通就诊）
    @NotBlank(message = "需求类型不能为空")
    private String demandType;

    // 紧急程度（必填：1-普通 2-紧急 3-急诊）
    @NotNull(message = "紧急程度不能为空")
    private Integer emergencyLevel;

    // 手术名称（对应页面1的“手术名称”输入框，选填）
    private String surgeryName;

    // 症状列表（对应页面1的“症状选择”多选框，选填）
    private List<String> symptoms;

    // 其他需求（对应页面1的“其他需求”输入框，选填）
    private String otherRequirement;
}