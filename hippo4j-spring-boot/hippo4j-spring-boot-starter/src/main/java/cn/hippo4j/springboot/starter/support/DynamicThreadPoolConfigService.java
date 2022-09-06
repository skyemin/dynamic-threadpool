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

package cn.hippo4j.springboot.starter.support;

import cn.hippo4j.common.model.ThreadPoolParameterInfo;
import cn.hippo4j.common.model.register.DynamicThreadPoolRegisterParameter;
import cn.hippo4j.common.model.register.DynamicThreadPoolRegisterWrapper;
import cn.hippo4j.common.toolkit.Assert;
import cn.hippo4j.common.toolkit.BooleanUtil;
import cn.hippo4j.common.toolkit.JSONUtil;
import cn.hippo4j.common.web.base.Result;
import cn.hippo4j.common.web.exception.ServiceException;
import cn.hippo4j.core.executor.DynamicThreadPoolWrapper;
import cn.hippo4j.core.executor.manage.GlobalNotifyAlarmManage;
import cn.hippo4j.core.executor.manage.GlobalThreadPoolManage;
import cn.hippo4j.core.executor.support.service.AbstractDynamicThreadPoolService;
import cn.hippo4j.message.dto.NotifyConfigDTO;
import cn.hippo4j.message.service.Hippo4jBaseSendMessageService;
import cn.hippo4j.message.service.ThreadPoolNotifyAlarm;
import cn.hippo4j.springboot.starter.config.BootstrapProperties;
import cn.hippo4j.springboot.starter.core.ClientWorker;
import cn.hippo4j.springboot.starter.core.DynamicThreadPoolSubscribeConfig;
import cn.hippo4j.springboot.starter.event.ApplicationCompleteEvent;
import cn.hippo4j.springboot.starter.notify.ServerNotifyConfigBuilder;
import cn.hippo4j.springboot.starter.remote.HttpAgent;
import com.google.common.collect.Lists;
import cn.hippo4j.springboot.starter.remote.HttpAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static cn.hippo4j.common.constant.Constants.REGISTER_DYNAMIC_THREAD_POOL_PATH;

/**
 * Dynamic thread-pool config service.
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicThreadPoolConfigService extends AbstractDynamicThreadPoolService implements ApplicationListener<ApplicationCompleteEvent> {

    private final HttpAgent httpAgent;

    private final ClientWorker clientWorker;

    private final BootstrapProperties properties;

    private final ServerNotifyConfigBuilder notifyConfigBuilder;

    private final Hippo4jBaseSendMessageService hippo4jBaseSendMessageService;

    private final DynamicThreadPoolSubscribeConfig dynamicThreadPoolSubscribeConfig;

    @Override
    public ThreadPoolExecutor registerDynamicThreadPool(DynamicThreadPoolRegisterWrapper registerWrapper) {
        ThreadPoolExecutor dynamicThreadPoolExecutor = registerExecutor(registerWrapper);
        subscribeConfig(registerWrapper);
        putNotifyAlarmConfig(registerWrapper);
        return dynamicThreadPoolExecutor;
    }

    @Override
    public void onApplicationEvent(ApplicationCompleteEvent event) {
        clientWorker.notifyApplicationComplete();
    }

    private ThreadPoolExecutor registerExecutor(DynamicThreadPoolRegisterWrapper registerWrapper) {
        DynamicThreadPoolRegisterParameter registerParameter = registerWrapper.getDynamicThreadPoolRegisterParameter();
        checkThreadPoolParameter(registerParameter);
        String threadPoolId = registerParameter.getThreadPoolId();
        try {
            failDynamicThreadPoolRegisterWrapper(registerWrapper);
            Result registerResult = httpAgent.httpPost(REGISTER_DYNAMIC_THREAD_POOL_PATH, registerWrapper);
            if (registerResult == null || !registerResult.isSuccess()) {
                throw new ServiceException("Dynamic thread pool registration returns error.");
            }
        } catch (Throwable ex) {
            log.error("Dynamic thread pool registration execution error: {}", threadPoolId, ex);
            throw ex;
        }
        ThreadPoolParameterInfo parameter = JSONUtil.parseObject(JSONUtil.toJSONString(registerParameter), ThreadPoolParameterInfo.class);
        ThreadPoolExecutor dynamicThreadPoolExecutor = buildDynamicThreadPoolExecutor(registerParameter);
        DynamicThreadPoolWrapper dynamicThreadPoolWrapper = DynamicThreadPoolWrapper.builder()
                .threadPoolId(threadPoolId)
                .executor(dynamicThreadPoolExecutor)
                .build();
        GlobalThreadPoolManage.register(threadPoolId, parameter, dynamicThreadPoolWrapper);
        return dynamicThreadPoolExecutor;
    }

    private void subscribeConfig(DynamicThreadPoolRegisterWrapper registerWrapper) {
        dynamicThreadPoolSubscribeConfig.subscribeConfig(registerWrapper.getDynamicThreadPoolRegisterParameter().getThreadPoolId());
    }

    private void putNotifyAlarmConfig(DynamicThreadPoolRegisterWrapper registerWrapper) {
        DynamicThreadPoolRegisterParameter registerParameter = registerWrapper.getDynamicThreadPoolRegisterParameter();
        ThreadPoolNotifyAlarm threadPoolNotifyAlarm = new ThreadPoolNotifyAlarm(
                BooleanUtil.toBoolean(String.valueOf(registerParameter.getIsAlarm())),
                registerParameter.getActiveAlarm(),
                registerParameter.getCapacityAlarm());
        GlobalNotifyAlarmManage.put(registerParameter.getThreadPoolId(), threadPoolNotifyAlarm);
        Map<String, List<NotifyConfigDTO>> builderNotify = notifyConfigBuilder.getAndInitNotify(Lists.newArrayList(registerParameter.getThreadPoolId()));
        hippo4jBaseSendMessageService.putPlatform(builderNotify);
    }

    private void checkThreadPoolParameter(DynamicThreadPoolRegisterParameter registerParameter) {
        Assert.isTrue(!registerParameter.getThreadPoolId().contains("+"), "The thread pool contains sensitive characters.");
    }

    private void failDynamicThreadPoolRegisterWrapper(DynamicThreadPoolRegisterWrapper registerWrapper) {
        registerWrapper.setTenantId(properties.getNamespace());
        registerWrapper.setItemId(properties.getItemId());
    }
}
