package org.example.unity;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class TextProcessor {

    /**
     * 医疗文本智能分段
     */
    public static List<String> segmentMedicalText(String text) {
        List<String> paragraphs = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return paragraphs;
        }

        // 1. 按句子分割（中英文标点）
        String[] sentences = text.split("(?<=[。！？\\.!?])");

        StringBuilder currentPara = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;

            currentPara.append(trimmed);

            // 分段条件：医疗建议、警告、列表等
            boolean shouldBreak = false;

            // 医疗关键词触发分段
            String[] breakKeywords = {"建议", "如果", "需要", "可以", "注意", "警告",
                    "包括", "例如", "原因", "症状", "治疗"};
            for (String keyword : breakKeywords) {
                if (trimmed.contains(keyword) && trimmed.length() > 2) {
                    shouldBreak = true;
                    break;
                }
            }

            // 段落长度触发
            if (currentPara.length() > 100) {
                shouldBreak = true;
            }

            if (shouldBreak && currentPara.length() > 20) {
                paragraphs.add(currentPara.toString());
                currentPara = new StringBuilder();
            }
        }

        // 添加最后一段
        if (currentPara.length() > 0) {
            paragraphs.add(currentPara.toString());
        }

        // 确保至少有一个段落
        if (paragraphs.isEmpty()) {
            paragraphs.add(text);
        }

        return paragraphs;
    }

    public String paragraphsToText(List<String> answerSegments) {
        return null;
    }
}