package com.wbs.project.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成和验证JWT令牌
 */
@Component
public class JwtUtil {

    // JWT密钥（生产环境应该从配置文件中读取）
    private static final String SECRET_KEY = "wbs-project-management-system-secret-key-for-jwt-token-generation-must-be-long-enough";

    // Token有效期：7天
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;

    // 生成密钥
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * 生成JWT令牌（兼容旧版本，tokenVersion 默认为 0）
     */
    public String generateToken(String userId, String role) {
        return generateToken(userId, role, 0L);
    }

    /**
     * 生成JWT令牌（角色管理 v2：携带 tokenVersion 用于失效控制）
     * @param userId 用户ID
     * @param role 用户角色
     * @param tokenVersion 当前用户的 token_version（角色变更时会 +1,与历史 token 不一致则 401）
     * @return JWT令牌
     */
    public String generateToken(String userId, String role, long tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("tokenVersion", tokenVersion);

        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSignKey())
                .compact();
    }

    /**
     * 从JWT令牌中提取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public String extractUserId(String token) {
        Claims claims = extractClaims(token);
        return claims.get("userId", String.class);
    }

    /**
     * 从JWT令牌中提取用户角色
     * @param token JWT令牌
     * @return 用户角色
     */
    public String extractRole(String token) {
        Claims claims = extractClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * 从JWT令牌中提取 tokenVersion（角色管理 v2）
     * 旧 token 中无该字段时默认 0
     * @param token JWT令牌
     * @return tokenVersion
     */
    public long extractTokenVersion(String token) {
        Claims claims = extractClaims(token);
        Object v = claims.get("tokenVersion");
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 验证JWT令牌是否有效
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 提取JWT令牌中的所有声明
     * @param token JWT令牌
     * @return 声明
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 检查令牌是否即将过期（剩余时间少于1天）
     * @param token JWT令牌
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = extractClaims(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            long remainingTime = expiration.getTime() - now.getTime();
            return remainingTime < (24 * 60 * 60 * 1000); // 少于1天
        } catch (Exception e) {
            return true;
        }
    }
}
