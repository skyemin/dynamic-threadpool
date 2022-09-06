/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.hippo4j.springboot.starter.toolkit;

import cn.hippo4j.common.toolkit.JSONUtil;
import cn.hippo4j.common.web.exception.ServiceException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HttpClient util.
 */
@Slf4j
public class HttpClientUtil {

    @Resource
    private OkHttpClient hippo4JOkHttpClient;

    private MediaType jsonMediaType = MediaType.parse("application/json; charset=utf-8");

    private static int HTTP_OK_CODE = 200;

    /**
     * Get.
     *
     * @param url
     * @return
     */
    @SneakyThrows
    public String get(String url) {
        try {
            return new String(doGet(url), "utf-8");
        } catch (Exception e) {
            log.error("httpGet 调用失败. {}", url, e);
            throw e;
        }
    }

    /**
     * Get request, supports adding query string.
     *
     * @param url
     * @param queryString
     * @return
     */
    public String get(String url, Map<String, String> queryString) {
        String fullUrl = buildUrl(url, queryString);
        return get(fullUrl);
    }

    /**
     * Deserialize directly after getting Json.
     *
     * @param url
     * @param clazz
     * @return
     */
    public <T> T restApiGet(String url, Class<T> clazz) {
        String resp = get(url);
        return JSONUtil.parseObject(resp, clazz);
    }

    /**
     * Call health check.
     *
     * @param url
     * @param clazz
     * @param <T>
     * @return
     */
    @SneakyThrows
    public <T> T restApiGetHealth(String url, Class<T> clazz) {
        String resp = new String(doGet(url), "utf-8");
        return JSONUtil.parseObject(resp, clazz);
    }

    /**
     * Get request, supports query string.
     *
     * @param url
     * @param queryString
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T restApiGet(String url, Map<String, String> queryString, Class<T> clazz) {
        String fullUrl = buildUrl(url, queryString);
        String resp = get(fullUrl);
        return JSONUtil.parseObject(resp, clazz);
    }

    /**
     * Rest interface Post call.
     *
     * @param url
     * @param body
     * @return
     */
    public String restApiPost(String url, Object body) {
        try {
            return doPost(url, body);
        } catch (Exception e) {
            log.error("httpPost 调用失败. {} message: {}", url, e.getMessage());
            throw e;
        }
    }

    /**
     * Rest interface Post call. Deserialize the return value directly.
     *
     * @param url
     * @param body
     * @return
     */
    public <T> T restApiPost(String url, Object body, Class<T> clazz) {
        String resp = restApiPost(url, body);
        return JSONUtil.parseObject(resp, clazz);
    }

    /**
     * Constructs a complete Url from the query string.
     *
     * @param url
     * @param queryString
     * @return
     */
    public String buildUrl(String url, Map<String, String> queryString) {
        if (null == queryString) {
            return url;
        }
        StringBuilder builder = new StringBuilder(url);
        boolean isFirst = true;
        for (Map.Entry<String, String> entry : queryString.entrySet()) {
            String key = entry.getKey();
            if (key != null && entry.getValue() != null) {
                if (isFirst) {
                    isFirst = false;
                    builder.append("?");
                } else {
                    builder.append("&");
                }
                builder.append(key)
                        .append("=")
                        .append(queryString.get(key));
            }
        }
        return builder.toString();
    }

    @SneakyThrows
    private String doPost(String url, Object body) {
        String jsonBody = JSONUtil.toJSONString(body);
        RequestBody requestBody = RequestBody.create(jsonMediaType, jsonBody);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        try (Response resp = hippo4JOkHttpClient.newCall(request).execute()) {
            try (ResponseBody responseBody = resp.body()) {
                if (resp.code() != HTTP_OK_CODE) {
                    String msg = String.format("HttpPost 响应 code 异常. [code] %s [url] %s [body] %s", resp.code(), url, jsonBody);
                    throw new ServiceException(msg);
                }
                return responseBody.string();
            }
        }
    }

    @SneakyThrows
    private byte[] doGet(String url) {
        Request request = new Request.Builder().get().url(url).build();
        try (Response resp = hippo4JOkHttpClient.newCall(request).execute()) {
            try (ResponseBody responseBody = resp.body()) {
                if (resp.code() != HTTP_OK_CODE) {
                    String msg = String.format("HttpGet 响应 code 异常. [code] %s [url] %s", resp.code(), url);
                    throw new ServiceException(msg);
                }
                return responseBody.bytes();
            }
        }
    }

    @SneakyThrows
    public <T> T restApiGetByThreadPool(String url, Map<String, String> headers, Map<String, String> paramValues, Long readTimeoutMs, Class<T> clazz) {
        String buildUrl = buildUrl(url, paramValues);
        Request.Builder builder = new Request.Builder().get();
        if (!CollectionUtils.isEmpty(headers)) {
            builder.headers(Headers.of(headers));
        }
        Request request = builder.url(buildUrl).build();
        Call call = hippo4JOkHttpClient.newCall(request);
        // TODO Plan to optimize the timout api because its version is too high.
        call.timeout().timeout(readTimeoutMs, TimeUnit.MILLISECONDS);
        try (Response resp = call.execute()) {
            try (ResponseBody responseBody = resp.body()) {
                if (resp.code() != HTTP_OK_CODE) {
                    String msg = String.format("HttpGet 响应 code 异常. [code] %s [url] %s", resp.code(), url);
                    log.error(msg);
                    throw new ServiceException(msg);
                }
                return JSONUtil.parseObject(responseBody.string(), clazz);
            }
        }
    }

    @SneakyThrows
    public <T> T restApiPostByThreadPool(String url, Map<String, String> headers, Map<String, String> paramValues, Long readTimeoutMs, Class<T> clazz) {
        String buildUrl = buildUrl(url, paramValues);
        Request request = new Request.Builder()
                .url(buildUrl)
                .headers(Headers.of(headers))
                .post(RequestBody.create(jsonMediaType, ""))
                .build();
        Call call = hippo4JOkHttpClient.newCall(request);
        // TODO Plan to optimize the timout api because its version is too high.
        call.timeout().timeout(readTimeoutMs, TimeUnit.MILLISECONDS);
        try (Response resp = call.execute()) {
            try (ResponseBody responseBody = resp.body()) {
                if (resp.code() != HTTP_OK_CODE) {
                    String msg = String.format("HttpPost 响应 code 异常. [code] %s [url] %s.", resp.code(), url);
                    log.error(msg);
                    throw new ServiceException(msg);
                }
                return JSONUtil.parseObject(responseBody.string(), clazz);
            }
        }
    }
}
