package com.xht.inspirahivebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author：xht
 */
@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = -2250062449364913328L;
    private String userAccount;
    private String userPassword;
}
