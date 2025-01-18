package com.xht.inspirahivebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xht.inspirahivebackend.model.dto.picture.PictureQueryRequest;
import com.xht.inspirahivebackend.model.dto.picture.PictureUploadRequest;
import com.xht.inspirahivebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xht.inspirahivebackend.model.entity.User;
import com.xht.inspirahivebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author 59812
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-01-16 20:26:58
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 获取单个图片
     * @param picture
     * @param httpServletRequest
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest httpServletRequest);


    /**
     * 分页获取图片
     * @param picturePage
     * @param httpServletRequest
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest httpServletRequest);

    /**
     * 校验图片参数
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 把java对象转化成mybatis需要的查询条件
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);
}
