package com.xht.inspirahivebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xht.inspirahivebackend.model.dto.user.UserQueryRequest;
import com.xht.inspirahivebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xht.inspirahivebackend.model.vo.LoginUserVO;
import com.xht.inspirahivebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author xhtht
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-01-06 15:35:45
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return 新用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户退出
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获得登录用户脱敏后信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户脱敏后信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取用户列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 把java对象转化成mybatis需要的查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 密码加密
     * @param password
     * @return
     */
    String getEncryptPassword(String password);

    /**
     * 是否为管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);

}
