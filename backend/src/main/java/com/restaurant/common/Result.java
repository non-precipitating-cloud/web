package com.restaurant.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果封装
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 状态码: 200=成功, 其他=失败 */
    private Integer code;
    /** 提示信息 */
    private String msg;
    /** 响应数据 */
    private T data;

    // ==================== 成功 ====================

    public static <T> Result<T> ok() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    // ==================== 失败 ====================

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    // ==================== 常用错误码 ====================

    public static <T> Result<T> unauthorized() {
        return new Result<>(401, "请先登录", null);
    }

    public static <T> Result<T> forbidden() {
        return new Result<>(403, "无权限访问", null);
    }

    public static <T> Result<T> notFound() {
        return new Result<>(404, "资源不存在", null);
    }

    public static <T> Result<T> badRequest(String msg) {
        return new Result<>(400, msg, null);
    }
}
