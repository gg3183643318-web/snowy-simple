package com.example.snowydemo.common;

public class CommonResult<T> {

    private final int code;
    private final String message;
    private final T data;

    private CommonResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResult<T> data(T data) {
        return new CommonResult<>(200, "success", data);
    }

    public static <T> CommonResult<T> created(T data) {
        return new CommonResult<>(201, "created", data);
    }

    public static <T> CommonResult<T> error(int code, String message) {
        return new CommonResult<>(code, message, null);
    }

    public static CommonResult<String> ok() {
        return new CommonResult<>(200, "success", "ok");
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
