package com.xht.inspirahivebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xht.inspirahivebackend.annotation.AuthCheck;
import com.xht.inspirahivebackend.api.aliyunai.AliYunAiApi;
import com.xht.inspirahivebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.xht.inspirahivebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.xht.inspirahivebackend.api.imagesearch.ImageSearchApiFacade;
import com.xht.inspirahivebackend.api.imagesearch.model.ImageSearchResult;
import com.xht.inspirahivebackend.common.BaseResponse;
import com.xht.inspirahivebackend.common.DeleteRequest;
import com.xht.inspirahivebackend.common.ResultUtils;
import com.xht.inspirahivebackend.constant.UserConstant;
import com.xht.inspirahivebackend.exception.BusinessException;
import com.xht.inspirahivebackend.exception.ErrorCode;
import com.xht.inspirahivebackend.exception.ThrowUtils;
import com.xht.inspirahivebackend.manager.auth.SpaceUserAuthManager;
import com.xht.inspirahivebackend.manager.auth.StpKit;
import com.xht.inspirahivebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.xht.inspirahivebackend.manager.auth.model.SpaceUserPermission;
import com.xht.inspirahivebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xht.inspirahivebackend.model.dto.picture.*;
import com.xht.inspirahivebackend.model.entity.Picture;
import com.xht.inspirahivebackend.model.entity.Space;
import com.xht.inspirahivebackend.model.entity.User;
import com.xht.inspirahivebackend.model.enums.PictureReviewStatusEnum;
import com.xht.inspirahivebackend.model.vo.PictureTagCategory;
import com.xht.inspirahivebackend.model.vo.PictureVO;
import com.xht.inspirahivebackend.service.PictureService;
import com.xht.inspirahivebackend.service.SpaceService;
import com.xht.inspirahivebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author：xht
 */
@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    .expireAfterWrite(5L, TimeUnit.MINUTES)   // 缓存 5 分钟移除
                    .build();

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     *
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 批量上传图片（仅管理员可用）
     *
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Integer batch = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(batch);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest httpServletRequest) {
        // 判断数据是否存在
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     *
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        // 请求是否为空
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取图片数据，转为实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 图片参数校验
        pictureService.validPicture(picture);
        // 判断图片是否存在
        long picId = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(picId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片信息
     *
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 根据id获取图片全部信息（仅管理员可用）
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    /**
     * 根据id获取图片（封装类）
     *
     * @param id
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            boolean permission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!permission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            // 已经改为使用注解鉴权
            // pictureService.checkPictureAuth(loginUser, picture);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, httpServletRequest);
        pictureVO.setPermissionList(permissionList);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     *
     * @param pictureQueryRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            // 公开图库
            // 普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 私有空间
            boolean permission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!permission, ErrorCode.NO_AUTH_ERROR);
//            User loginUser = userService.getLoginUser(httpServletRequest);
//            Space space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
//            }
        }
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, httpServletRequest));
    }

    /**
     * 分页获取图片列表（封装类，有缓存）
     *
     * @param pictureQueryRequest
     * @param httpServletRequest
     * @return
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest httpServletRequest) {
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能看到审核通过的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 多级缓存
        // 1.查询本地缓存
        // 构建缓存 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = "InspiraHive:listPictureVOByPage:" + hashKey;
        String cacheValue = LOCAL_CACHE.getIfPresent(cacheKey);
        // 缓存命中
        if (cacheValue != null) {
            Page<PictureVO> cachedPage = JSONUtil.toBean(cacheValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 2.查询 redis 缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String cacheValue2 = opsForValue.get(cacheKey);
        if (cacheValue2 != null) {
            // 命中 redis，存入本地缓存并返回
            LOCAL_CACHE.put(cacheKey, cacheValue2);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cacheValue2, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 3.缓存未命中，查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, httpServletRequest);
        // 4.更新缓存
        String cacheValue3 = JSONUtil.toJsonStr(pictureVOPage);
        // 设置过期时间，（一个范围，防止redis缓存雪崩）
        opsForValue.set(cacheKey, cacheValue3, 300 + RandomUtil.randomInt(0, 300), TimeUnit.MINUTES);
        LOCAL_CACHE.put(cacheKey, cacheValue3);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 返回常用标签分类
     *
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     *
     * @param pictureReviewRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginuser = userService.getLoginUser(httpServletRequest);
        pictureService.doPictureReview(pictureReviewRequest, loginuser);
        return ResultUtils.success(true);
    }

    /**
     * 以图搜图
     *
     * @param searchPictureByPictureRequest
     * @return
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 调整后缀，支持百度以图搜图
        String webpUrl = oldPicture.getUrl();
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        int lastDotIndex1 = webpUrl.lastIndexOf('.');
        int lastDotIndex2 = thumbnailUrl.lastIndexOf('.');
        String suffix2 = thumbnailUrl.substring(lastDotIndex2);
        String originUrl = webpUrl.substring(0, lastDotIndex1) + suffix2;
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(originUrl);
        return ResultUtils.success(resultList);
    }


    /**
     * 根据颜色搜索图片
     *
     * @param searchPictureByColorRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        String picColor = searchPictureByColorRequest.getPicColor();
        List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, userService.getLoginUser(httpServletRequest));
        return ResultUtils.success(pictureVOList);
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginuser = userService.getLoginUser(httpServletRequest);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginuser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     *
     * @param taskId
     * @return
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }

}
