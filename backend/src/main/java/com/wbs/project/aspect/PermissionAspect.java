package com.wbs.project.aspect;

import com.wbs.project.annotation.DataScope;
import com.wbs.project.annotation.RequirePermission;
import com.wbs.project.annotation.RequireRole;
import com.wbs.project.entity.User;
import com.wbs.project.enums.UserRole;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.mapper.UserMapper;
import com.wbs.project.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 权限 AOP 切面（角色管理 v2）
 *
 * 处理两类注解:
 * - @RequireRole({"admin","dept-project-manager"})  : 角色白名单
 * - @RequirePermission(value="...", dataScope=..., projectIdParam=...)
 *      : 细粒度权限码 + 数据范围
 *
 * 角色 userId 从 request 取出（AuthInterceptor 已写入），无需重复解析 token
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;
    private final UserMapper userMapper;

    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint jp, RequireRole requireRole) {
        String userId = currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(401, "用户不存在");
        }
        for (String r : requireRole.value()) {
            if (r.equals(u.getRole())) {
                return;
            }
        }
        log.warn("角色校验失败: userId={}, role={}, required={}", userId, u.getRole(), String.join(",", requireRole.value()));
        throw new BusinessException(403, "无角色权限");
    }

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint jp, RequirePermission requirePermission) {
        String userId = currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }

        boolean allowed = switch (requirePermission.dataScope()) {
            case GLOBAL   -> permissionService.hasPermission(roleOf(userId), requirePermission.value());
            case PROJECT  -> {
                String projectId = extractStringParam(jp, requirePermission.projectIdParam());
                if (projectId == null) {
                    throw new BusinessException(400, "缺少 projectId 参数");
                }
                yield permissionService.canEditProject(userId, projectId);
            }
            case DEPT     -> {
                String deptCode = extractStringParam(jp, requirePermission.deptCodeParam());
                if (deptCode == null) {
                    throw new BusinessException(400, "缺少 deptCode 参数");
                }
                yield permissionService.isDeptManager(userId, deptCode);
            }
            case SELF     -> {
                String selfUserId = extractStringParam(jp, requirePermission.selfParam());
                yield userId.equals(selfUserId);
            }
            case PROGRESS -> {
                String taskId = extractStringParam(jp, requirePermission.taskIdParam());
                if (taskId == null) {
                    throw new BusinessException(400, "缺少 taskId 参数");
                }
                yield permissionService.canEditTaskProgress(userId, taskId);
            }
        };

        if (!allowed) {
            log.warn("权限校验失败: userId={}, code={}, scope={}", userId, requirePermission.value(), requirePermission.dataScope());
            throw new BusinessException(403, "无操作权限: " + requirePermission.value());
        }
    }

    private String currentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        Object v = req.getAttribute("userId");
        return v == null ? null : v.toString();
    }

    private String roleOf(String userId) {
        User u = userMapper.selectById(userId);
        return u == null ? null : u.getRole();
    }

    /**
     * 从方法参数中按名称提取 String 类型参数
     * 支持 @PathVariable / @RequestParam 风格的参数命名
     */
    private String extractStringParam(JoinPoint jp, String paramName) {
        if (paramName == null || paramName.isEmpty()) {
            return null;
        }
        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = jp.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            String name = parameters[i].getName();
            // 兼容 Spring @PathVariable / @RequestParam 显式命名
            org.springframework.web.bind.annotation.PathVariable pv = parameters[i].getAnnotation(
                    org.springframework.web.bind.annotation.PathVariable.class);
            if (pv != null && !pv.value().isEmpty()) {
                name = pv.value();
            } else {
                org.springframework.web.bind.annotation.RequestParam rq = parameters[i].getAnnotation(
                        org.springframework.web.bind.annotation.RequestParam.class);
                if (rq != null && !rq.value().isEmpty()) {
                    name = rq.value();
                }
            }
            if (paramName.equals(name) && i < args.length && args[i] != null) {
                return args[i].toString();
            }
        }
        return null;
    }
}
