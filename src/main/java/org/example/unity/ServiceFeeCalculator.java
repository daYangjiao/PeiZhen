package org.example.unity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 陪诊服务费用计算工具类
 * 实现各种服务类型的费用计算逻辑
 */
public class ServiceFeeCalculator {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceFeeCalculator.class);
    
    // 费用标准常量
    private static final BigDecimal BASE_PRICE = new BigDecimal("50");      // 普通陪诊起步价（2小时）
    private static final BigDecimal EXTEND_PRICE = new BigDecimal("30");    // 普通陪诊延长费（元/小时）
    private static final BigDecimal POST_CARE_PRICE = new BigDecimal("45"); // 术后护理（元/小时）
    private static final BigDecimal URGENT_FEE = new BigDecimal("100");     // 急诊陪同加急费
    private static final BigDecimal HOME_VISIT_FEE = new BigDecimal("30");  // 上门陪诊上门费
    
    /**
     * 服务类型枚举
     */
    public enum ServiceType {
        @ApiModelProperty("普通陪诊")
        NORMAL(1, "普通陪诊"),
        
        @ApiModelProperty("术后护理")
        POST_CARE(2, "术后护理"),
        
        @ApiModelProperty("急诊陪同")
        EMERGENCY(3, "急诊陪同"),
        
        @ApiModelProperty("上门陪诊")
        HOME_VISIT(4, "上门陪诊");
        
        private final int code;
        private final String description;
        
        ServiceType(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static ServiceType fromCode(int code) {
            for (ServiceType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的服务类型代码: " + code);
        }
    }
    
    /**
     * 费用计算结果
     */
    @Data
    @ApiModel(description = "费用计算结果")
    public static class FeeCalculationResult {
        @ApiModelProperty("总费用")
        private BigDecimal totalFee;
        
        @ApiModelProperty("费用明细列表")
        private List<FeeItem> feeItems;
        
        @ApiModelProperty("计算依据说明")
        private String calculationBasis;
        
        public FeeCalculationResult() {
            this.feeItems = new ArrayList<>();
        }
        
        public void addFeeItem(String itemName, BigDecimal amount, String description) {
            FeeItem item = new FeeItem();
            item.setItemName(itemName);
            item.setAmount(amount);
            item.setDescription(description);
            this.feeItems.add(item);
        }
    }
    
    /**
     * 费用明细项
     */
    @Data
    @ApiModel(description = "费用明细项")
    public static class FeeItem {
        @ApiModelProperty("费用项目名称")
        private String itemName;
        
        @ApiModelProperty("费用金额")
        private BigDecimal amount;
        
        @ApiModelProperty("费用说明")
        private String description;
    }
    
    /**
     * 计算陪诊服务费用（默认包含计算依据）
     * @param serviceType 服务类型编号
     * @param durationHours 服务时长（小时）
     * @return 费用计算结果
     */
    public static FeeCalculationResult calculateFee(int serviceType, double durationHours) {
        return calculateFee(serviceType, durationHours, true);
    }
    
    /**
     * 计算陪诊服务费用
     * @param serviceType 服务类型编号
     * @param durationHours 服务时长（小时）
     * @param includeBasis 是否包含计算依据
     * @return 费用计算结果
     */
    public static FeeCalculationResult calculateFee(int serviceType, double durationHours, boolean includeBasis) {
        ServiceType type = ServiceType.fromCode(serviceType);
        BigDecimal hours = BigDecimal.valueOf(durationHours);
        
        logger.info("开始计算费用: 服务类型={}, 时长={}小时", type.getDescription(), durationHours);
        
        FeeCalculationResult result = new FeeCalculationResult();
        
        // 设置计算依据
        if (includeBasis) {
            result.setCalculationBasis(String.format("根据服务类型编号%d(%s)和时长%.1f小时计算", 
                serviceType, type.getDescription(), durationHours));
        }
        
        switch (type) {
            case NORMAL:
                calculateNormalServiceFee(result, hours);
                break;
            case POST_CARE:
                calculatePostCareFee(result, hours);
                break;
            case EMERGENCY:
                calculateEmergencyFee(result, hours);
                break;
            case HOME_VISIT:
                calculateHomeVisitFee(result, hours);
                break;
            default:
                throw new IllegalArgumentException("不支持的服务类型: " + serviceType);
        }
        
        logger.info("费用计算完成: 总费用={}", result.getTotalFee());
        return result;
    }
    
    /**
     * 计算普通陪诊费用
     * 起步价：50元（2小时），延长费：30元/小时
     */
    private static void calculateNormalServiceFee(FeeCalculationResult result, BigDecimal hours) {
        // 不足2小时按2小时计费
        BigDecimal actualHours = hours.compareTo(new BigDecimal("2")) < 0 ? 
            new BigDecimal("2") : hours;
        
        // 基础费用（前2小时）
        result.addFeeItem("起步价", BASE_PRICE, "2小时最低起约时长");
        
        // 延长费用
        if (actualHours.compareTo(new BigDecimal("2")) > 0) {
            BigDecimal extraHours = actualHours.subtract(new BigDecimal("2"));
            // 不足1小时按1小时计费
            BigDecimal roundedExtraHours = extraHours.setScale(0, BigDecimal.ROUND_UP);
            BigDecimal extendFee = roundedExtraHours.multiply(EXTEND_PRICE);
            
            if (extendFee.compareTo(BigDecimal.ZERO) > 0) {
                result.addFeeItem("延长费", extendFee, 
                    roundedExtraHours + "小时 × " + EXTEND_PRICE + "元/小时");
            }
        }
        
        // 计算总费用
        BigDecimal total = result.getFeeItems().stream()
            .map(FeeItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setTotalFee(total);
    }
    
    /**
     * 计算术后护理费用
     * 45元/小时
     */
    private static void calculatePostCareFee(FeeCalculationResult result, BigDecimal hours) {
        // 不足1小时按1小时计费
        BigDecimal roundedHours = hours.setScale(0, BigDecimal.ROUND_UP);
        BigDecimal totalFee = roundedHours.multiply(POST_CARE_PRICE);
        
        result.addFeeItem("术后护理费", totalFee, 
            roundedHours + "小时 × " + POST_CARE_PRICE + "元/小时");
        result.setTotalFee(totalFee);
    }
    
    /**
     * 计算急诊陪同费用
     * 按普通陪诊计算后加100元加急费
     */
    private static void calculateEmergencyFee(FeeCalculationResult result, BigDecimal hours) {
        // 先计算普通陪诊费用
        calculateNormalServiceFee(result, hours);
        
        // 添加加急费
        result.addFeeItem("加急费", URGENT_FEE, "急诊陪同加急服务费");
        
        // 重新计算总费用
        BigDecimal total = result.getFeeItems().stream()
            .map(FeeItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setTotalFee(total);
    }
    
    /**
     * 计算上门陪诊费用
     * 普通陪诊计算后加30元上门费
     */
    private static void calculateHomeVisitFee(FeeCalculationResult result, BigDecimal hours) {
        // 先计算普通陪诊费用
        calculateNormalServiceFee(result, hours);
        
        // 添加上门费
        result.addFeeItem("上门费", HOME_VISIT_FEE, "上门陪诊服务费");
        
        // 重新计算总费用
        BigDecimal total = result.getFeeItems().stream()
            .map(FeeItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setTotalFee(total);
    }
    
    /**
     * 格式化费用显示
     * @param fee 费用金额
     * @return 格式化的费用字符串
     */
    public static String formatFee(BigDecimal fee) {
        return String.format("%.2f元", fee);
    }
}