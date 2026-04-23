package org.example.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class SysAdmin {

    private Integer id;

    private String name;

    private String phone;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private Integer status;

    private String role;

    private Date createTime;

    private Date updateTime;

    private Date lastLoginTime;
}
