package com.xht.inspirahivebackend.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xht.inspirahivebackend.exception.ErrorCode;

/**
 * @Author：xht
 */
public class ResultUtils {
    /**
     * 响应成功
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200,data,"ok");
    }

    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code,null,message);
    }

    /**
     * 失败
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode,String message) {
        return new BaseResponse<>(errorCode.getCode(),null,message);
    }

}
