package com.wbs.project.interceptor;

import com.wbs.project.entity.User;
import com.wbs.project.mapper.UserMapper;
import com.wbs.project.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT认证拦截器
 * 拦截所有请求，验证JWT令牌 + tokenVersion（角色管理 v2）
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    /**
     * 在处理请求之前进行认证检查
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 设置CORS响应头
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // 如果是OPTIONS请求，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 从请求头中获取Authorization
        String authHeader = request.getHeader("Authorization");

        // 检查Authorization是否存在
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
            return false;
        }

        // 提取Token
        String token = authHeader.substring(7);

        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\",\"data\":null}");
            return false;
        }

        // Token有效，将用户信息存入请求属性
        String userId = jwtUtil.extractUserId(token);
        String role = jwtUtil.extractRole(token);
        long tokenVersionInJwt = jwtUtil.extractTokenVersion(token);

        // 角色管理 v2: 校验 tokenVersion,角色/管辖范围变更后强制重新登录
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"账号不存在\",\"data\":null}");
            return false;
        }
        long currentTokenVersion = user.getTokenVersion() == null ? 0L : user.getTokenVersion().longValue();
        if (currentTokenVersion != tokenVersionInJwt) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"账号权限已变更,请重新登录\",\"data\":null}");
            return false;
        }

        request.setAttribute("userId", userId);
        request.setAttribute("userRole", role);

        return true;
    }
}
