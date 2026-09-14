package com.bis.intelliguide.aspect;

import com.bis.intelliguide.model.AuditLog;
import com.bis.intelliguide.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Automatically logs every successful admin write endpoint to audit_log — this is
 * what "logged automatically via an aspect/interceptor, not manually in every
 * controller" (see security checklist in the build prompt) means in practice.
 * Any method in a com.bis.intelliguide.controller.admin.* class whose name starts
 * with create/update/publish/reject/submit/restore/import/resolve/invite/role/delete
 * is treated as a write action.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning(pointcut = "execution(* com.bis.intelliguide.controller.admin..*.*(..))")
    public void logAdminAction(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        boolean isWrite = methodName.matches("(?i)^(create|update|publish|reject|submit|restore|import|resolve|invite|changeRole|promote|delete).*");
        if (!isWrite) return;

        String adminUserId = "unknown";
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            adminUserId = auth.getPrincipal().toString();
        }

        String entityType = joinPoint.getSignature().getDeclaringTypeName()
                .replace("com.bis.intelliguide.controller.admin.", "")
                .replace("Controller", "");

        String entityId = joinPoint.getArgs().length > 0 ? String.valueOf(joinPoint.getArgs()[0]) : "N/A";

        auditLogRepository.save(AuditLog.builder()
                .adminUserId(adminUserId)
                .entityType(entityType)
                .entityId(entityId)
                .action(methodName)
                .timestamp(Instant.now())
                .build());
    }
}
