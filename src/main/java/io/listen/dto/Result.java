package io.listen.dto;

import io.listen.enums.ResponseStatusEnum;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Result<T> {

    private boolean success; // 是否成功

    private int code; // 状态码（HTTP 状态码或自定义错误码）

    private String message; // 提示信息

    private T data; // 成功时的返回数据

    // 私有构造函数，强制通过静态方法创建
    private Result(boolean success, int code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功响应
    public static <T> Result<T> success(T data) {
        return new Result<>(true, ResponseStatusEnum.OK.code(), ResponseStatusEnum.OK.message(), data);
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(true, ResponseStatusEnum.OK.code(), message, data);
    }

    // 失败响应
    public static <T> Result<T> failure(ResponseStatusEnum status) {
        return new Result<>(false, status.code(), status.message(), null);
    }

    public static <T> Result<T> failure(String message) {
        return new Result<>(false, ResponseStatusEnum.ERROR.code(), message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
