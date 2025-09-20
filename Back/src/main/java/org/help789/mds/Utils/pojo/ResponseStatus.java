package org.help789.mds.Utils.pojo;


public enum ResponseStatus {

    SUCCESS(200 ,"Success"),
    BAD_REQUEST(400, "客户端请求错误"),
    UNAUTHORIZED(401 ,"用户认证失败"),
    FORBIDDEN(403 ,"权限不足"),
    NOT_FOUND(404 ,"请求资源不存在"),
    SERVICE_ERROR(500, "服务器内部错误"),
    PARAM_INVALID(1000, "无效的参数"),
    ;

    public final Integer code;

    public final String message;

    ResponseStatus(Integer code, String msg) {
        this.code = code;
        this.message = msg;
    }
}