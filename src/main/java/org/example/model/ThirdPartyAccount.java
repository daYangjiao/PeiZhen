package org.example.model;

import lombok.Data;

import java.util.Date;

@Data
public class ThirdPartyAccount {

    private Long id;

    private String principalType;

    private Integer principalId;

    private String provider;

    private String platform;

    private String openid;

    private String unionid;

    private Date createTime;

    private Date updateTime;
}
