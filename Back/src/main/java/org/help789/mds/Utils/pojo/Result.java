package org.help789.mds.Utils.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
public class Result<T> {

    /**
     * 对业务本身的答复
     */
    private Boolean reply = false;

    private String message; //提示信息

    private T data; //响应数据

    private PageInfo pageInfo; //分页信息

    public static <E> Result<E> success(String msg, E data){
        return new Result<>(true, msg, data, null);
    }

    public static <E> Result<E> success(E data, PageInfo pageInfo){
        return new Result<>(true, ResponseStatus.SUCCESS.message, data, pageInfo);
    }

    public static <E> Result<E> success(E data){
        return new Result<>(true, ResponseStatus.SUCCESS.message, data, null);
    }

    public static Result success(){
        return new Result(true, ResponseStatus.SUCCESS.message, null, null);
    }

    public static Result failed(){
        return new Result(false, "服务器错误", null, null);
    }

    public static Result failed(String message){
        return new Result(false, message, null, null);
    }
}
