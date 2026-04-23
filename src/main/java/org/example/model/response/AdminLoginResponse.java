package org.example.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.entity.SysAdmin;

@Data
@AllArgsConstructor
public class AdminLoginResponse {

    private String token;

    private SysAdmin userInfo;
}
