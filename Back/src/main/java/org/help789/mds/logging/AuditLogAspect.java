package org.help789.mds.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.help789.mds.Entity.AuditLog;
import org.help789.mds.Utils.ThreadLocalUtil;
import org.help789.mds.repository.AuditLogRepository;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository repo;

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(logAction)")
    public Object around(ProceedingJoinPoint pjp, LogAction logAction) throws Throwable {
        boolean success = true;
        String error = null;
        Object result;

        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable ex) {
            success = false;
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            throw ex;
        } finally {
            try {
                saveLog(pjp, logAction, success, error);
            } catch (Exception e) {
                log.warn("保存操作日志失败: {}", e.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint pjp, LogAction anno, boolean success, String error) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Object[] args = pjp.getArgs();

        Long userId = null;
        String userAccount = null;
        try {
            Object idObj = ThreadLocalUtil.getCurrentUserId();
            if (idObj instanceof Number n) userId = n.longValue();
            else if (idObj != null) userId = Long.valueOf(String.valueOf(idObj));
            userAccount = ThreadLocalUtil.getCurrentUserAccount();
        } catch (Exception ignored) {}

        String detail = resolveDetail(anno.detail(), method, args);

        AuditLog logRow = AuditLog.builder()
                .userId(userId)
                .userAccount(userAccount)
                .action(anno.value())
                .detail(detail)
                .success(success)
                .errorMsg(success ? null : error)
                .time(LocalDateTime.now())
                .build();

        repo.save(logRow);
    }

    private String resolveDetail(String spel, Method method, Object[] args) {
        if (spel == null || spel.isBlank()) return null;
        try {
            EvaluationContext ctx = new StandardEvaluationContext();
            String[] names = nameDiscoverer.getParameterNames(method);
            for (int i = 0; i < args.length; i++) {
                String key = (names != null && i < names.length && names[i] != null) ? names[i] : "p" + i;
                ctx.setVariable(key, args[i]);
            }
            ctx.setVariable("args", Arrays.asList(args));
            Expression exp = parser.parseExpression(spel);
            Object val = exp.getValue(ctx);
            return val == null ? null : val.toString();
        } catch (Exception e) {
            return spel; // 渲染失败则返回原文
        }
    }
}

