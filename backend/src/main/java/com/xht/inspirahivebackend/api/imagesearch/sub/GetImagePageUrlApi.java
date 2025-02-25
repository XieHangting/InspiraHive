package com.xht.inspirahivebackend.api.imagesearch.sub;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.xht.inspirahivebackend.exception.BusinessException;
import com.xht.inspirahivebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址（step 1）
 *
 * @Author：xht
 */
@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取图片页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        // 1. 准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        String acsToken = "1740458013286_1740461114976_+U0UYomfTFMmaeq7JvFMCUrXH5DIZ652j6ywLm5+D8+6jQ/fzM9JaCrasIjD7Zk2ZL3Cni7C3IxsGdw9Mmv1XVRmSGoCDH5zrsDTHCnaBpBB1hfZeOjo4zwT9eYnqF72aJQnGXRvfje+1a5iOsid1ZyLQDvoAxY+Za80oH7DRDuBa99NiOoir63nM80ntl54O/HnUVkDcD6HDKEsVsG06zC/CBDJPHs2weYMl1/7P5LbARh/HYu9iXc5Botvn3zF2YjqMz8BPhqy4pU6PAYOoSqhm4IyRazl9XFFbdyQUqLZmOyz7jfHpW9JcgG9qHj6vpYDfnOKqWdnOZmfjKs4qRcp0XMr6/I8PLzi2WUCEZpota35EZGATWj7CM6TPQg9E4rlWp5Tzr6b0o42V75QdJVKc7dR74UkWpjsGQYl9L1L1JcynbzQEcgfm5kq3CVTtyKISQdsALW74DVktmCvog==";
        try {
            // 2. 发送 POST 请求到百度接口
            HttpResponse response = HttpRequest.post(url)
                    .form(formData)
                    .header("Acs-Token", acsToken)
                    .timeout(5000)
                    .execute();
            // 判断响应状态
            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            // 解析响应
            String responseBody = response.body();
            Map<String, Object> result = JSONUtil.toBean(responseBody, Map.class);

            // 3. 处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            // 对 URL 进行解码
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            if (searchResultUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://www.baidu.com/img/flexible/logo/pc/result.png";
        String result = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果 URL：" + result);
    }
}

