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
            "/user/login", "/user/login-email", "/user/send-email-code", "/user/register",
            "/error", "/v3/api-docs", "/swagger-ui", "/swagger-ui.html"
    );

    @Resource
    private JWTservice jwtservice;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        // CORS 预检请求
        if (HttpMethod.OPTIONS.matches(req.getMethod())) {
            setCorsHeaders(req, resp);
            resp.setStatus(HttpStatus.OK.value());
            return false;
        }

        // 白名单放行
        String uri = req.getRequestURI();
        if (isWhitelisted(uri)) return true;

        // 从 Authorization 取 token（兼容 Bearer 前缀）
        String token = req.getHeader("Authorization");
        if (token == null || token.isBlank()) return unauthorized(req, resp, "Missing Authorization header");
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) token = token.substring(7).trim();

        // 调用 JWTservice 校验签名 & 过期
        try {
            Map<String, Object> claims = jwtservice.parseJWTToken(token); // 验证失败会抛 JWTVerificationException
            req.setAttribute("claims", claims); // 给后续控制器用
            return true; // ✅ 验证通过，放行
        } catch (JWTVerificationException e) {
            return unauthorized(req, resp, "Invalid or expired token");
        } catch (Exception e) {
            return unauthorized(req, resp, "Token verification error");
        }
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
