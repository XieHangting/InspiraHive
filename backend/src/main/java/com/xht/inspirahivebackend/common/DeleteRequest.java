package com.xht.inspirahivebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author：xht
 * 删除请求包装类
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
