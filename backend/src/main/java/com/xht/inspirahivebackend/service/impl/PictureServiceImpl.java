package com.xht.inspirahivebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xht.inspirahivebackend.exception.BusinessException;
import com.xht.inspirahivebackend.exception.ErrorCode;
import com.xht.inspirahivebackend.exception.ThrowUtils;
import com.xht.inspirahivebackend.manager.CosManager;
import com.xht.inspirahivebackend.manager.upload.FilePictureUpload;
import com.xht.inspirahivebackend.manager.upload.PictureUploadTemplate;
import com.xht.inspirahivebackend.manager.upload.UrlPictureUpload;
import com.xht.inspirahivebackend.model.dto.file.UploadPictureResult;
import com.xht.inspirahivebackend.model.dto.picture.*;
import com.xht.inspirahivebackend.model.entity.Picture;
import com.xht.inspirahivebackend.model.entity.Space;
import com.xht.inspirahivebackend.model.entity.User;
import com.xht.inspirahivebackend.model.enums.PictureReviewStatusEnum;
import com.xht.inspirahivebackend.model.vo.PictureVO;
import com.xht.inspirahivebackend.model.vo.UserVO;
import com.xht.inspirahivebackend.service.PictureService;
import com.xht.inspirahivebackend.mapper.PictureMapper;
import com.xht.inspirahivebackend.service.SpaceService;
import com.xht.inspirahivebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 59812
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-01-16 20:26:58
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

//    @Resource
//    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    // 上传图片
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null){
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验权限，是否为空间的管理员
            if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有空间权限");
            }
            // 校验存储上限
            if (space.getTotalCount() >= space.getMaxCount()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间图片数量已达到上限");
            }
            if (space.getTotalSize() >= space.getMaxSize()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }
        // 判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原有图片一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }
        // 上传图片，得到图片信息
        // 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null){
            // 公共图库
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        }else{
            // 私有空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据 inputSource 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");
            if (finalSpaceId != null) {
                // 更新空间的使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        return PictureVO.objToVo(picture);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest httpServletRequest) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 查询当前用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest httpServletRequest) {
        // 获取分页数据
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getPages(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象转封装类
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        pictureVOList.forEach(pictureVO -> {
                    Long userId = pictureVO.getUserId();
                    User user = null;
                    if (userIdUserListMap.containsKey(userId)) {
                        user = userIdUserListMap.get(userId).get(0);
                    }
                    pictureVO.setUser(userService.getUserVO(user));
                }
        );
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 1.参数校验
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        // 转为枚举类
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2.判断图片是否存在
        Picture picture = this.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3.判断图片审核状态是否重复
        if (picture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 4.审核图片，操作数据库
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewTime(new Date());
        updatePicture.setReviewerId(loginUser.getId());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 获取图片元素
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次处理上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            pictureUploadRequest.setFileUrl(fileUrl);
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 删除图片
        cosManager.deleteObject(oldPicture.getUrl());
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 更新空间的使用额度，释放额度
            if (oldPicture.getSpaceId() != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, oldPicture.getSpaceId())
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限
        checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }


}




