package org.example.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.model.User;

@Data
@AllArgsConstructor
public class AdminLoginResponse {

    private String token;

    private User userInfo;
}
