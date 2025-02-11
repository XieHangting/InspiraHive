package com.xht.inspirahivebackend.api.imagesearch;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;

import com.google.gson.*;

/**
 * @Author：xht
 */
public class ImageSearchByBing {
    static String subscriptionKey = "enter key here";
    static String host = "https://api.bing.microsoft.com";
    static String path = "/v7.0/images/search";

    /**
     * 通过Bing API搜索图片
     *
     * @param searchQuery 搜索关键词
     * @return 返回第一个图片结果的缩略图URL
     * @throws IOException 如果请求失败或解析响应时出错
     */
    public static String searchImage(String searchQuery) throws IOException {
        // 构造请求URL
        String urlString = host + path + "?q=" + URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString());
        URL url = new URL(urlString);

        // 打开HTTPS连接
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);

        // 发送请求并获取响应
        try (InputStream stream = connection.getInputStream();
             Scanner scanner = new Scanner(stream).useDelimiter("\\A")) {
            String response = scanner.hasNext() ? scanner.next() : "";

            // 解析JSON响应
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(response).getAsJsonObject();

            // 获取总结果数和第一个结果的缩略图URL
            String total = json.get("totalEstimatedMatches").getAsString();
            System.out.println("Total estimated matches: " + total);

            JsonArray results = json.getAsJsonArray("value");
            if (results.size() == 0) {
                throw new IOException("No results found for the search query: " + searchQuery);
            }

            JsonObject firstResult = results.get(0).getAsJsonObject();
            String resultURL = firstResult.get("thumbnailUrl").getAsString();
            return resultURL;
        } finally {
            connection.disconnect(); // 关闭连接
        }
    }

    /**
     * 主方法，用于测试图片搜索功能
     */
    public static void main(String[] args) {
        try {
            String searchQuery = "tropical ocean"; // 搜索关键词
            String thumbnailUrl = searchImage(searchQuery); // 调用搜索方法
            System.out.println("First image thumbnail URL: " + thumbnailUrl);
        } catch (IOException e) {
            System.err.println("Error during image search: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
