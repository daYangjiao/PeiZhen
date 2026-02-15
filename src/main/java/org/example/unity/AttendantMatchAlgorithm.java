package org.example.unity;

import org.example.model.Attendant;
import org.example.model.MatchedAttendantVO;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI导诊-陪诊师匹配算法（基于标签相似度+紧急程度加权）
 */
@Component
public class AttendantMatchAlgorithm {

    /**
     * 核心匹配逻辑
     * @param demandTags 用户需求标签（如["膝关节手术","术后护理"]）
     * @param availableAttendants 可用陪诊师列表
     * @param emergencyLevel 紧急程度（1-3）
     * @return 匹配后的陪诊师（按匹配度降序）
     */
    public List<MatchedAttendantVO> matchAttendants(
            Set<String> demandTags,
            List<Attendant> availableAttendants,
            Integer emergencyLevel) {
        if (availableAttendants.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 计算每个陪诊师的匹配度
        return availableAttendants.stream().map(attendant -> {
                    MatchedAttendantVO matchedVO = new MatchedAttendantVO();
                    // 仅调用VO中存在的字段set方法
                    matchedVO.setAttendantId(attendant.getId()); // 对应VO的attendantId
                    matchedVO.setAttendantName(attendant.getName()); // 对应VO的attendantName
                    matchedVO.setAttendantPhone(Optional.ofNullable(attendant.getPhone()).orElse("")); // 对应VO的attendantPhone
                    matchedVO.setSpecialty(Optional.ofNullable(attendant.getProfessionalField()).orElse("通用陪诊")); // 对应VO的specialty
                    matchedVO.setScore(calculateMatchScore(demandTags, attendant, emergencyLevel)); // 对应VO的score
                    matchedVO.setExperience(buildExperienceDesc(attendant)); // 对应VO的experience

                    return matchedVO;
                })
                // 2. 按匹配度（score）降序排序
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());
    }

    /**
     * 计算匹配度（适配现有VO的score字段）
     */
    private Double calculateMatchScore(Set<String> demandTags, Attendant attendant, Integer emergencyLevel) {
        // 解析陪诊师的专业标签
        Set<String> attendantTags = Optional.ofNullable(attendant.getProfessionalField())
                .map(field -> Arrays.stream(field.split(","))
                        .map(String::trim)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());

        // 计算标签匹配度（交集数量/需求标签数量 * 100）
        int intersectionSize = (int) demandTags.stream()
                .filter(attendantTags::contains)
                .count();
        double baseScore = demandTags.isEmpty() ? 60.0 : (intersectionSize * 100.0) / demandTags.size();

        // 紧急程度加权（紧急/急诊+10分）
        double emergencyBonus = emergencyLevel >= 2 ? 10.0 : 0.0;

        // 最终匹配度（不超过100）
        return Math.min(baseScore + emergencyBonus, 100.0);
    }

    /**
     * 构建经验描述（适配现有VO的experience字段）
     */
    private String buildExperienceDesc(Attendant attendant) {
        // 示例：拼接陪诊师经验+擅长领域
        String field = Optional.ofNullable(attendant.getProfessionalField()).orElse("");
        int experienceYears = (int) Optional.ofNullable(attendant.getExperienceYears()).orElse(0);
        return experienceYears + "年陪诊经验，擅长：" + (field.isEmpty() ? "通用陪诊" : field);
    }
}