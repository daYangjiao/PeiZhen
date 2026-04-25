package org.example.model.response;

import lombok.Data;

@Data
public class AdminWorkbenchSummaryResponse {

    private long disputeOrderCount;

    private long attendantReviewCount;

    private long myClaimCount;
}
