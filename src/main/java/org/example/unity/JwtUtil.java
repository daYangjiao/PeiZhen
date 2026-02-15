package org.example.unity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    // 使用更安全的密钥生成方式
    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME = 86400000; // 24小时

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
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
        
        // 构建基础声明
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        
        // 添加额外声明
        if (additionalClaims != null) {
            claims.putAll(additionalClaims);
        }
        
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SECRET_KEY)
                .compact();
        
        logger.debug("为用户 {} 生成Token，过期时间: {}", userId, expiryDate);
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
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // 检查是否过期
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                logger.warn("Token已过期");
                throw new RuntimeException("Token已过期");
            }
            
            return Integer.parseInt(claims.getSubject());
        } catch (Exception e) {
            logger.error("Token解析失败: {}", e.getMessage());
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
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}