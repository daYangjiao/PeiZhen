package org.example.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendantRatingSummary {

    private BigDecimal score;

    private Integer evaluationCount;

    private Integer praiseRate;
}
