package org.help789.mds.Utils;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ThreadLocalUtil {

    // 每个线程独立存一份 Map<String, Object>
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 创建用户信息 Map
     */
    public static Map<String, Object> createUserInfoClaim(Long id, String account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("account", account);
        return claims;
    }

    /**
     * 获取当前线程的 claims，没有则新建
     */
    private static Map<String, Object> getOrCreate() {
        Map<String, Object> claims = THREAD_LOCAL.get();
        if (claims == null) {
            claims = new HashMap<>();
            THREAD_LOCAL.set(claims);
        }
        return claims;
    }

    /**
     * 设置 claims（合并/覆盖）
     */
    public static void setClaim(Map<String, Object> claims2) {
        Map<String, Object> claims1 = getOrCreate();
        claims1.putAll(claims2);
    }

    /**
     * 获取当前用户 ID
     */
    public static Long getCurrentUserId() {
        Map<String, Object> claims = getOrCreate();
        Object id = claims.get("id");
        if (id instanceof Long) {
            return (Long) id;
        } else if (id instanceof Integer) {
            return ((Integer) id).longValue();
        } else {
            return null; // 没有或者类型不对
        }
    }

    /**
     * 获取当前用户账号
     */
    public static String getCurrentUserAccount() {
        Map<String, Object> claims = getOrCreate();
        Object account = claims.get("account");
        return account instanceof String ? (String) account : null;
    }

    /**
     * 清理 ThreadLocal，避免内存泄漏
     */
    public static void clear() {
        THREAD_LOCAL.remove();
    }
}


