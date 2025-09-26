package org.help789.mds.Utils;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ThreadLocalUtil {

    //ThreadLocal会自动为每个使用该变量的线程提供独立的变量副本
    private static final ThreadLocal THREAD_LOCAL = new ThreadLocal();

    public static Map<String, Object> createUserInfoClaim(Integer id, String name){
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("id", id);
        claims.put("name", name);
        return claims;
    }

    private static <T> T get(){
        return (T) THREAD_LOCAL.get();
    }

    private static Map<String, Object> getOrCreate(){
        Map<String, Object> claims = get();
        if(claims == null){
            claims = new HashMap<String, Object>();
            set(claims);
        }
        return claims;
    }

    /**
     * 获取当前用户ID
     * @return
     */
    public static Integer getCurrentUserId(){
        Map<String, Object> claims = get();
        int userId = Integer.MIN_VALUE;
        try {
            userId = (int) claims.get("id");
        }catch (Exception e){
            log.warn("未能获取到当前登录用户ID: {}", e.getMessage());
        }
        return userId;
    }

    /**
     * 获取当前用户账户名
     * @return
     */
    public static String getCurrentUserName(){
        Map<String, Object> claims = get();
        String userName = null;
        try {
            userName = (String) claims.get("name");
        }catch (Exception e){
            log.warn("未能获取到当前登录用户名称: {}", e.getMessage());
        }
        return userName;
    }

    private static void set(Object vlue){
        THREAD_LOCAL.set(vlue);
    }

    /**
     * 合并/覆盖
     * @param claims2
     */
    public static void setClaim(Map<String, Object> claims2){
        Map<String, Object> claims1 = getOrCreate();
        for (String k : claims2.keySet()) {
            claims1.put(k, claims2.get(k));
        }
    }

    /**
     * 使用完后必须删除，避免线程复用时访问到上一次的数据
     * 在请求完成的拦截器中调用
     */
    public static void clear(){
        THREAD_LOCAL.remove();
    }
}


