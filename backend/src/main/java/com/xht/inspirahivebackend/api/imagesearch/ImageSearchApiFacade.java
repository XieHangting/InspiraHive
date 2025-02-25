package com.xht.inspirahivebackend.api.imagesearch;

import com.xht.inspirahivebackend.api.imagesearch.model.ImageSearchResult;
import com.xht.inspirahivebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.xht.inspirahivebackend.api.imagesearch.sub.GetImageListApi;
import com.xht.inspirahivebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
//        String result = GetImagePageUrlApi.getImagePageUrl("https://www.codefather.cn/logo.png");
//        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(result);
//        System.out.println("搜索成功，结果 URL：" + imageFirstUrl);
        String url = "https://inspirahive-1336210730.cos.ap-shanghai.myqcloud.com/space/1892848216259616770/2025-02-25_q6dv5xv2m018ajha.webp";
        String newUrl = url.replaceAll("\\.[^.]+$", ".");
        System.out.println(newUrl);
        List<ImageSearchResult> imageList = searchImage(newUrl);

        System.out.println("结果列表" + imageList);
    }
}
