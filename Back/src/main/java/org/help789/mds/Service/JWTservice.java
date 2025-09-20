package org.help789.mds.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class JWTservice {

    // 可改成你项目里实际的配置键；这里给了默认值防止本地未配置时报错
    @Value("${mds.user.login-expires-minutes:120}")
    private int loginExpiresMinutes;

    @Value("${mds.user.jwt-token-key:CHANGE_ME_TO_A_LONG_RANDOM_SECRET}")
    private String jwtTokenKey;

    /**
     * 生成 JWT
     */
    public String getJWTToken(Map<String, Object> claims) {
        long expireMs = loginExpiresMinutes * 60L * 1000L;
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + expireMs))
                .sign(Algorithm.HMAC256(jwtTokenKey));
    }

    /**
     * 解析并验证 JWT（过期或被篡改会抛 JWTVerificationException）
     */
    public Map<String, Object> parseJWTToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(jwtTokenKey)).build();
        DecodedJWT decoded = verifier.verify(token);
        return decoded.getClaim("claims").asMap();
    }

    /* ========= 兼容保留：旧方法名（GWT -> JWT），避免现有代码立刻报错 ========= */

    /** @deprecated 请改用 {@link #getJWTToken(Map)} */
    @Deprecated
    public String getGWTToken(Map<String, Object> claims) {
        return getJWTToken(claims);
    }

    /** @deprecated 请改用 {@link #parseJWTToken(String)} */
    @Deprecated
    public Map<String, Object> parseGWTToken(String token) throws JWTVerificationException {
        return parseJWTToken(token);
    }
}
