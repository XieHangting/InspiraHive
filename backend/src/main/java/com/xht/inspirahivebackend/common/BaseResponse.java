package com.xht.inspirahivebackend.common;

import com.xht.inspirahivebackend.exception.ErrorCode;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;

/**
 * @Author：xht
 */
@Data
public class BaseResponse<T> implements Serializable {
    private int code;
    private T data;
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, null);
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
