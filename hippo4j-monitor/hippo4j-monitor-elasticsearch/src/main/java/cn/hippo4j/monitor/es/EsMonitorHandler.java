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

package cn.hippo4j.monitor.es;

import cn.hippo4j.common.config.ApplicationContextHolder;
import cn.hippo4j.common.model.ThreadPoolRunStateInfo;
import cn.hippo4j.common.toolkit.JSONUtil;
import cn.hippo4j.core.executor.state.ThreadPoolRunStateHandler;
import cn.hippo4j.monitor.es.model.EsThreadPoolRunStateInfo;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hippo4j.monitor.base.AbstractDynamicThreadPoolMonitor;
import cn.hippo4j.monitor.base.MonitorTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elastic-search monitor handler.
 */
@Slf4j
public class EsMonitorHandler extends AbstractDynamicThreadPoolMonitor {

    public EsMonitorHandler(ThreadPoolRunStateHandler threadPoolRunStateHandler) {
        super(threadPoolRunStateHandler);
    }

    private AtomicBoolean isIndexExist = null;

    @Override
    protected void execute(ThreadPoolRunStateInfo poolRunStateInfo) {
        EsThreadPoolRunStateInfo esThreadPoolRunStateInfo = new EsThreadPoolRunStateInfo();
        BeanUtil.copyProperties(poolRunStateInfo, esThreadPoolRunStateInfo);
        Environment environment = ApplicationContextHolder.getInstance().getEnvironment();
        String indexName = environment.getProperty("es.thread-pool-state.index.name", "thread-pool-state");
        String applicationName = environment.getProperty("spring.application.name", "application");
        if (!this.isExists(indexName)) {
            List<String> rawMapping = FileUtil.readLines(new File(Thread.currentThread().getContextClassLoader().getResource("mapping.json").getPath()), StandardCharsets.UTF_8);
            String mapping = String.join(" ", rawMapping);
            // if index doesn't exsit, this function may try to create one, but recommend to create index manually.
            this.createIndex(indexName, "_doc", mapping, null, null, null);
        }
        esThreadPoolRunStateInfo.setApplicationName(applicationName);
        esThreadPoolRunStateInfo.setId(indexName + "-" + System.currentTimeMillis());
        this.log2Es(esThreadPoolRunStateInfo, indexName);
    }

    public void log2Es(EsThreadPoolRunStateInfo esThreadPoolRunStateInfo, String indexName) {
        RestHighLevelClient client = EsClientHolder.getClient();
        try {
            IndexRequest request = new IndexRequest(indexName, "_doc");
            request.id(esThreadPoolRunStateInfo.getId());
            String stateJson = JSONUtil.toJSONString(esThreadPoolRunStateInfo);
            request.source(stateJson, XContentType.JSON);
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            log.info("write thread-pool state to es, id is :{}", response.getId());
        } catch (Exception ex) {
            log.error("es index error, the exception was thrown in create index. name:{},type:{},id:{}. {} ",
                    indexName,
                    "_doc",
                    esThreadPoolRunStateInfo.getId(),
                    ex);
        }
    }

    public synchronized boolean isExists(String index) {
        // cache check result
        if (Objects.isNull(isIndexExist)) {
            boolean exists = false;
            GetIndexRequest request = new GetIndexRequest(index);
            try {
                RestHighLevelClient client = EsClientHolder.getClient();
                exists = client.indices().exists(request, RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("check es index fail");
            }
            isIndexExist = new AtomicBoolean(exists);
        }
        return isIndexExist.get();
    }

    public void createIndex(String index, String type, String mapping, Integer shards, Integer replicas, String alias) {
        RestHighLevelClient client = EsClientHolder.getClient();
        boolean acknowledged = false;
        CreateIndexRequest request = new CreateIndexRequest(index);
        if (StringUtils.hasText(mapping)) {
            request.mapping(type, mapping, XContentType.JSON);
        }
        if (!Objects.isNull(shards) && !Objects.isNull(replicas)) {
            request.settings(Settings.builder()
                    .put("index.number_of_shards", shards) // 5
                    .put("index.number_of_replicas", replicas));// 1
        }
        if (StringUtils.hasText(alias)) {
            request.alias(new Alias(alias));
        }
        try {
            CreateIndexResponse createIndexResponse = client.indices().create(request, RequestOptions.DEFAULT);
            acknowledged = createIndexResponse.isAcknowledged();
        } catch (IOException e) {
            log.error("create es index exception", e);
        }
        if (acknowledged) {
            log.info("create es index success");
            isIndexExist.set(true);
        } else {
            log.error("create es index fail");
            throw new RuntimeException("cannot auto create thread-pool state es index");
        }
    }

    @Override
    public String getType() {
        return MonitorTypeEnum.ELASTICSEARCH.name().toLowerCase();
    }
}
