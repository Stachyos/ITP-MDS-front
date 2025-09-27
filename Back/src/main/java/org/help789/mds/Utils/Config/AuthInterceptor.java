package org.help789.mds.Utils.Config;

import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.help789.mds.Service.JWTservice;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> WHITELIST = Set.of(
            "/api/user/login", "/api/user/login-email", "/api/user/send-email-code", "/api/user/register",
            "/error", "/v3/api-docs", "/swagger-ui", "/swagger-ui.html"
    );

    @Resource
    private JWTservice jwtservice;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        // 1) CORS 预检：放行（返回 true 让链路继续也可以；你现在的返回 false 也行）
        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            setCorsHeaders(req, resp);
            resp.setStatus(HttpStatus.OK.value());
            return false;
        }

        // 2) 白名单放行
        String uri = req.getRequestURI();
        if (isWhitelisted(uri)) return true;

        // 3) 读取并裁剪 Bearer token
        String token = req.getHeader("Authorization");
        if (token == null || token.isBlank()) return unauthorized(req, resp, "Missing Authorization header");
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) token = token.substring(7).trim();

        try {
            // 4) 解析并验证
            Map<String, Object> claims = jwtservice.parseJWTToken(token);

            // 5) 兼容 userId/id 两种键名，类型可能是 Integer/Long/String
            Object uidObj = claims.getOrDefault("userId", claims.get("id"));
            Long uid = null;
            if (uidObj instanceof Number) uid = ((Number) uidObj).longValue();
            else if (uidObj instanceof String s && s.matches("\\d+")) uid = Long.valueOf(s);

            String account = String.valueOf(claims.get("account"));

            // 6) 给 Controller 也留一份（可选）
            req.setAttribute("claims", claims);

            // 7) **关键：写入 ThreadLocal，供 Service 层使用**
            org.help789.mds.Utils.ThreadLocalUtil.setClaim(
                    org.help789.mds.Utils.ThreadLocalUtil.createUserInfoClaim(uid, account)
            );

            return true;
        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            return unauthorized(req, resp, "Invalid or expired token");
        } catch (Exception e) {
            return unauthorized(req, resp, "Token verification error");
        }
    }

    // **新增：请求结束清理 ThreadLocal，防止线程复用串号**
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        org.help789.mds.Utils.ThreadLocalUtil.clear();
    }

    private boolean isWhitelisted(String uri) {
        if (uri == null) return false;
        for (String allow : WHITELIST) {
            if (uri.equals(allow) || uri.startsWith(allow + "/")) return true;
        }
        return false;
    }

    private boolean unauthorized(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
        setCorsHeaders(req, resp);
        resp.setStatus(HttpStatus.UNAUTHORIZED.value());
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"reply\":false,\"message\":\"" + msg.replace("\"","\\\"") + "\",\"data\":null,\"pageInfo\":null}");
        return false;
    }

    private void setCorsHeaders(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");
        if (origin != null && !origin.isBlank()) resp.setHeader("Access-Control-Allow-Origin", origin);
        resp.setHeader("Vary", "Origin");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        resp.setHeader("Access-Control-Expose-Headers", "Authorization");
        resp.setHeader("Access-Control-Max-Age", "3600");
    }
}
