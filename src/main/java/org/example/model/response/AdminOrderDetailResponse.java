package org.example.model.response;

import lombok.Data;
import org.example.model.Order;
import org.example.model.User;

@Data
public class AdminOrderDetailResponse {

    private Order order;

    private User user;

    private User attendant;

    private String disputeResolverName;

    private String disputeResolverPhoneMasked;

    private String disputeResolverRole;
}
