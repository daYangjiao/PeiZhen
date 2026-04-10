package org.example.model.response;

import lombok.Data;

import java.util.Date;

@Data
public class AdminUserListItemResponse {

    private Integer id;

    private String name;

    private String phone;

    private Integer userType;

    private String userTypeLabel;

    private Integer status;

    private String statusLabel;

    private String avatar;

    private Date createTime;

    private long orderCount;
}
