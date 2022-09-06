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

package cn.hippo4j.console.controller;

import cn.hippo4j.common.toolkit.JSONUtil;
import cn.hippo4j.common.web.base.Result;
import cn.hippo4j.common.web.base.Results;
import cn.hippo4j.config.model.biz.adapter.ThreadPoolAdapterReqDTO;
import cn.hippo4j.config.model.biz.adapter.ThreadPoolAdapterRespDTO;
import cn.hippo4j.config.service.ThreadPoolAdapterService;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.http.HttpUtil;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

import static cn.hippo4j.common.constant.Constants.HTTP_EXECUTE_TIMEOUT;
import static cn.hippo4j.common.constant.Constants.REGISTER_ADAPTER_BASE_PATH;

/**
 * Thread-pool adapter controller.
 */
@AllArgsConstructor
@RestController("threadPoolAdapterConsoleController")
public class ThreadPoolAdapterController {

    private final ThreadPoolAdapterService threadPoolAdapterService;

    @GetMapping(REGISTER_ADAPTER_BASE_PATH + "/query")
    public Result<List<ThreadPoolAdapterRespDTO>> queryAdapterThreadPool(ThreadPoolAdapterReqDTO requestParameter) {
        List<ThreadPoolAdapterRespDTO> result = threadPoolAdapterService.query(requestParameter);
        return Results.success(result);
    }

    @GetMapping(REGISTER_ADAPTER_BASE_PATH + "/query/key")
    public Result<Set<String>> queryAdapterThreadPoolThreadPoolKey(ThreadPoolAdapterReqDTO requestParameter) {
        Set<String> result = threadPoolAdapterService.queryThreadPoolKey(requestParameter);
        return Results.success(result);
    }

    @PostMapping(REGISTER_ADAPTER_BASE_PATH + "/update")
    public Result<Void> updateAdapterThreadPool(@RequestBody ThreadPoolAdapterReqDTO requestParameter) {
        for (String each : requestParameter.getClientAddressList()) {
            String urlString = StrBuilder.create("http://", each, "/adapter/thread-pool/update").toString();
            HttpUtil.post(urlString, JSONUtil.toJSONString(requestParameter), HTTP_EXECUTE_TIMEOUT);
        }
        return Results.success();
    }

}
