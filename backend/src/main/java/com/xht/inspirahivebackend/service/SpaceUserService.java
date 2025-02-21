package com.xht.inspirahivebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xht.inspirahivebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.xht.inspirahivebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.xht.inspirahivebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xht.inspirahivebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 59812
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
*/
public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

}
