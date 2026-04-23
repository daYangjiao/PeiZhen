package org.example.unity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("dev-jwt-secret-change-me-please-1234567890", 86400000L);
    }

    @Test
    void generateTokenShouldMarkUserPrincipalByDefault() {
        String token = jwtUtil.generateToken(12);

        assertThat(jwtUtil.getPrincipalTypeFromToken(token)).isEqualTo("user");
        assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(12);
        assertThat(jwtUtil.getAdminIdFromToken(token)).isNull();
    }

    @Test
    void generateAdminTokenShouldMarkAdminPrincipal() {
        String token = jwtUtil.generateAdminToken(9, Map.of("scope", "admin-console"));

        assertThat(jwtUtil.getPrincipalTypeFromToken(token)).isEqualTo("admin");
        assertThat(jwtUtil.getAdminIdFromToken(token)).isEqualTo(9);
        assertThat(jwtUtil.getUserIdFromToken(token)).isNull();
        assertThat(jwtUtil.getAllClaimsFromToken(token).get("scope", String.class)).isEqualTo("admin-console");
    }
}
