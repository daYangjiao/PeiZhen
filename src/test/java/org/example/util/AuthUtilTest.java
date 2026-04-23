package org.example.util;

import org.example.unity.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuthUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("dev-jwt-secret-change-me-please-1234567890", 86400000L);
        AuthUtil authUtil = new AuthUtil();
        authUtil.setJwtUtil(jwtUtil);
    }

    @Test
    void getCurrentUserIdShouldIgnoreAdminToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtUtil.generateAdminToken(5, null));

        assertThat(AuthUtil.getCurrentUserId(request)).isNull();
    }

    @Test
    void getCurrentAdminIdShouldReadRequestAttributeFirst() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("currentAdminId", 7);

        assertThat(AuthUtil.getCurrentAdminId(request)).isEqualTo(7);
    }

    @Test
    void getCurrentAdminIdShouldIgnoreUserToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtUtil.generateToken(18));

        assertThat(AuthUtil.getCurrentAdminId(request)).isNull();
    }
}
