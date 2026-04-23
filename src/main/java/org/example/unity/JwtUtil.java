package org.example.unity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String CLAIM_PRINCIPAL_TYPE = "principalType";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ADMIN_ID = "adminId";
    private static final String PRINCIPAL_TYPE_USER = "user";
    private static final String PRINCIPAL_TYPE_ADMIN = "admin";

    private final SecretKey secretKey;
    private final long expirationTime;

    public JwtUtil(
            @Value("${jwt.secret:dev-jwt-secret-change-me-please-1234567890}") String jwtSecret,
            @Value("${jwt.expiration-ms:86400000}") long expirationTime
    ) {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET 长度至少需要 32 个字符");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
        this.expirationTime = expirationTime;
    }

    /**
     * 生成JWT Token
     * @param userId 用户ID
     * @return JWT Token字符串
     */
    public String generateToken(Integer userId) {
        return generateToken(userId, null);
    }
    
    /**
     * 生成包含额外信息的JWT Token
     * @param userId 用户ID
     * @param additionalClaims 额外的声明信息
     * @return JWT Token字符串
     */
    public String generateToken(Integer userId, Map<String, Object> additionalClaims) {
        return generateToken(userId, additionalClaims, expirationTime);
    }

    public String generateToken(Integer userId, Map<String, Object> additionalClaims, long customExpirationTime) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_PRINCIPAL_TYPE, PRINCIPAL_TYPE_USER);
        claims.put(CLAIM_USER_ID, userId);
        if (additionalClaims != null) {
            claims.putAll(additionalClaims);
        }
        return buildToken(userId, claims, customExpirationTime);
    }

    public String generateAdminToken(Integer adminId, Map<String, Object> additionalClaims) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_PRINCIPAL_TYPE, PRINCIPAL_TYPE_ADMIN);
        claims.put(CLAIM_ADMIN_ID, adminId);
        if (additionalClaims != null) {
            claims.putAll(additionalClaims);
        }
        return buildToken(adminId, claims, expirationTime);
    }

    private String buildToken(Integer subjectId, Map<String, Object> claims, long customExpirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + customExpirationTime);

        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(subjectId == null ? "" : subjectId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();

        logger.debug("为主体 {} 生成Token，过期时间: {}", subjectId, expiryDate);
        return token;
    }

    /**
     * 从Token中解析用户ID
     * @param token JWT Token
     * @return 用户ID
     * @throws RuntimeException 当Token无效或过期时抛出
     */
    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String principalType = normalizePrincipalType(claims);
            if (PRINCIPAL_TYPE_ADMIN.equals(principalType)) {
                return null;
            }
            Integer userId = toInteger(claims.get(CLAIM_USER_ID));
            if (userId != null) {
                return userId;
            }
            return toInteger(claims.getSubject());
        } catch (ExpiredJwtException e) {
            logger.warn("Token已过期");
            throw new RuntimeException("Token已过期");
        } catch (Exception e) {
            logger.warn("Token解析失败: {}", e.getMessage());
            throw new RuntimeException("无效的Token");
        }
    }

    public Integer getAdminIdFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            if (!PRINCIPAL_TYPE_ADMIN.equals(normalizePrincipalType(claims))) {
                return null;
            }
            Integer adminId = toInteger(claims.get(CLAIM_ADMIN_ID));
            if (adminId != null) {
                return adminId;
            }
            return toInteger(claims.getSubject());
        } catch (ExpiredJwtException e) {
            logger.warn("Token已过期");
            throw new RuntimeException("Token已过期");
        } catch (Exception e) {
            logger.warn("Token解析失败: {}", e.getMessage());
            throw new RuntimeException("无效的Token");
        }
    }

    public String getPrincipalTypeFromToken(String token) {
        try {
            return normalizePrincipalType(parseClaims(token));
        } catch (ExpiredJwtException e) {
            logger.warn("Token已过期");
            throw new RuntimeException("Token已过期");
        } catch (Exception e) {
            logger.warn("Token解析失败: {}", e.getMessage());
            throw new RuntimeException("无效的Token");
        }
    }
    
    /**
     * 验证Token是否有效
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            getUserIdFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从Token中获取所有声明信息
     * @param token JWT Token
     * @return Claims对象
     */
    public Claims getAllClaimsFromToken(String token) {
        return parseClaims(token);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String normalizePrincipalType(Claims claims) {
        String principalType = claims.get(CLAIM_PRINCIPAL_TYPE, String.class);
        return principalType == null || principalType.isBlank() ? PRINCIPAL_TYPE_USER : principalType;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }
}
