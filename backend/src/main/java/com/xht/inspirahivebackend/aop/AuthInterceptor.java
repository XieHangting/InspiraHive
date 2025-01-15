package com.xht.inspirahivebackend.aop;

import com.xht.inspirahivebackend.annotation.AuthCheck;
import com.xht.inspirahivebackend.exception.BusinessException;
import com.xht.inspirahivebackend.exception.ErrorCode;
import com.xht.inspirahivebackend.model.entity.User;
import com.xht.inspirahivebackend.model.enums.UserRoleEnum;
import com.xht.inspirahivebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @Author：xht
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 拦截器
     *
     * @param joinPoint
     * @param authCheck
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doIntercept(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String role = authCheck.role();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest attribute = ((ServletRequestAttributes) requestAttributes).getRequest();
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(role);
        // 不需要权限则放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        // 拦截，校验角色权限
        User loginUser = userService.getLoginUser(attribute);
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !userRoleEnum.equals(UserRoleEnum.ADMIN)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return joinPoint.proceed();
    }
}
