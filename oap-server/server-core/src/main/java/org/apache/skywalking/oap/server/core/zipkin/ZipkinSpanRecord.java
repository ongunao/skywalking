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
 *
 */

package org.apache.skywalking.oap.server.core.zipkin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.analysis.Stream;
import org.apache.skywalking.oap.server.core.analysis.record.Record;
import org.apache.skywalking.oap.server.core.analysis.worker.RecordStreamProcessor;
import org.apache.skywalking.oap.server.core.source.DefaultScopeDefine;
import org.apache.skywalking.oap.server.core.storage.annotation.BanyanDB;
import org.apache.skywalking.oap.server.core.storage.annotation.Column;
import org.apache.skywalking.oap.server.core.storage.annotation.SQLDatabase;
import org.apache.skywalking.oap.server.core.storage.annotation.SuperDataset;
import org.apache.skywalking.oap.server.core.storage.type.Convert2Entity;
import org.apache.skywalking.oap.server.core.storage.type.Convert2Storage;
import org.apache.skywalking.oap.server.core.storage.type.StorageBuilder;
import org.apache.skywalking.oap.server.library.util.BooleanUtils;
import org.apache.skywalking.oap.server.library.util.StringUtil;

@SuperDataset
@Stream(name = ZipkinSpanRecord.INDEX_NAME, scopeId = DefaultScopeDefine.ZIPKIN_SPAN, builder = ZipkinSpanRecord.Builder.class, processor = RecordStreamProcessor.class)
public class ZipkinSpanRecord extends Record {
    private static final Gson GSON = new Gson();
    public static final String INDEX_NAME = "zipkin_span";
    public static final String ADDITIONAL_QUERY_TABLE = "zipkin_query";
    public static final String TRACE_ID = "trace_id";
    public static final String SPAN_ID = "span_id";
    public static final String PARENT_ID = "parent_id";
    public static final String NAME = "name";
    public static final String DURATION = "duration";
    public static final String KIND = "kind";
    public static final String TIMESTAMP_MILLIS = "timestamp_millis";
    public static final String TIMESTAMP = "timestamp";
    public static final String LOCAL_ENDPOINT_SERVICE_NAME = "local_endpoint_service_name";
    public static final String LOCAL_ENDPOINT_IPV4 = "local_endpoint_ipv4";
    public static final String LOCAL_ENDPOINT_IPV6 = "local_endpoint_ipv6";
    public static final String LOCAL_ENDPOINT_PORT = "local_endpoint_port";
    public static final String REMOTE_ENDPOINT_SERVICE_NAME = "remote_endpoint_service_name";
    public static final String REMOTE_ENDPOINT_IPV4 = "remote_endpoint_ipv4";
    public static final String REMOTE_ENDPOINT_IPV6 = "remote_endpoint_ipv6";
    public static final String REMOTE_ENDPOINT_PORT = "remote_endpoint_port";
    public static final String ANNOTATIONS = "annotations";
    public static final String TAGS = "tags";
    public static final String DEBUG = "debug";
    public static final String SHARED = "shared";
    public static final String QUERY = "query";

    @Setter
    @Getter
    @Column(columnName = TRACE_ID)
    private String traceId;
    @Setter
    @Getter
    @Column(columnName = SPAN_ID)
    private String spanId;
    @Setter
    @Getter
    @Column(columnName = PARENT_ID)
    private String parentId;
    @Setter
    @Getter
    @Column(columnName = NAME)
    private String name;
    @Setter
    @Getter
    @Column(columnName = DURATION)
    private long duration;
    @Setter
    @Getter
    @Column(columnName = KIND)
    private String kind;
    @Setter
    @Getter
    @Column(columnName = TIMESTAMP_MILLIS)
    private long timestampMillis;
    @Setter
    @Getter
    @Column(columnName = TIMESTAMP)
    private long timestamp;
    @Setter
    @Getter
    @Column(columnName = LOCAL_ENDPOINT_SERVICE_NAME)
    @BanyanDB.ShardingKey(index = 0)
    private String localEndpointServiceName;
    @Setter
    @Getter
    @Column(columnName = LOCAL_ENDPOINT_IPV4, storageOnly = true)
    private String localEndpointIPV4;
    @Setter
    @Getter
    @Column(columnName = LOCAL_ENDPOINT_IPV6, storageOnly = true)
    private String localEndpointIPV6;
    @Setter
    @Getter
    @Column(columnName = LOCAL_ENDPOINT_PORT, storageOnly = true)
    private int localEndpointPort;
    @Setter
    @Getter
    @Column(columnName = REMOTE_ENDPOINT_SERVICE_NAME)
    private String remoteEndpointServiceName;
    @Setter
    @Getter
    @Column(columnName = REMOTE_ENDPOINT_IPV4, storageOnly = true)
    private String remoteEndpointIPV4;
    @Setter
    @Getter
    @Column(columnName = REMOTE_ENDPOINT_IPV6, storageOnly = true)
    private String remoteEndpointIPV6;
    @Setter
    @Getter
    @Column(columnName = REMOTE_ENDPOINT_PORT, storageOnly = true)
    private int remoteEndpointPort;
    @Setter
    @Getter
    @Column(columnName = ANNOTATIONS, storageOnly = true, length = 50000)
    private JsonObject annotations;
    @Setter
    @Getter
    @Column(columnName = TAGS, storageOnly = true, length = 50000)
    private JsonObject tags;
    @Setter
    @Getter
    @Column(columnName = DEBUG)
    private int debug;
    @Setter
    @Getter
    @Column(columnName = SHARED)
    private int shared;
    @Setter
    @Getter
    @Column(columnName = QUERY, indexOnly = true)
    @SQLDatabase.AdditionalEntity(additionalTables = {ADDITIONAL_QUERY_TABLE})
    private List<String> query;

    @Override
    public String id() {
        return spanId + Const.LINE + kind;
    }

    public static class Builder implements StorageBuilder<ZipkinSpanRecord> {
        @Override
        public ZipkinSpanRecord storage2Entity(final Convert2Entity converter) {
            ZipkinSpanRecord record = new ZipkinSpanRecord();
            record.setTraceId((String) converter.get(TRACE_ID));
            record.setSpanId((String) converter.get(SPAN_ID));
            record.setParentId((String) converter.get(PARENT_ID));
            record.setName((String) converter.get(NAME));
            record.setKind((String) converter.get(KIND));
            record.setTimestamp(((Number) converter.get(TIMESTAMP)).longValue());
            record.setTimestampMillis(((Number) converter.get(TIMESTAMP_MILLIS)).longValue());
            record.setDuration(((Number) converter.get(DURATION)).longValue());
            record.setLocalEndpointServiceName((String) converter.get(LOCAL_ENDPOINT_SERVICE_NAME));
            record.setLocalEndpointIPV4((String) converter.get(LOCAL_ENDPOINT_IPV4));
            record.setLocalEndpointIPV6((String) converter.get(LOCAL_ENDPOINT_IPV6));
            if (converter.get(LOCAL_ENDPOINT_PORT) != null) {
                record.setLocalEndpointPort(((Number) converter.get(LOCAL_ENDPOINT_PORT)).intValue());
            }
            record.setRemoteEndpointServiceName((String) converter.get(REMOTE_ENDPOINT_SERVICE_NAME));
            record.setRemoteEndpointIPV4((String) converter.get(REMOTE_ENDPOINT_IPV4));
            record.setRemoteEndpointIPV6((String) converter.get(REMOTE_ENDPOINT_IPV6));
            if (converter.get(REMOTE_ENDPOINT_PORT) != null) {
                record.setRemoteEndpointPort(((Number) converter.get(REMOTE_ENDPOINT_PORT)).intValue());
            }
            final String annotationsString = (String) converter.get(ANNOTATIONS);
            if (StringUtil.isNotEmpty(annotationsString)) {
                record.setAnnotations(GSON.fromJson(annotationsString, JsonObject.class));
            }
            final String tagsString = (String) converter.get(TAGS);
            if (StringUtil.isNotEmpty(tagsString)) {
                record.setTags(GSON.fromJson(tagsString, JsonObject.class));
            }
            if (converter.get(DEBUG) != null) {
                record.setDebug(((Number) converter.get(DEBUG)).intValue());
            }
            if (converter.get(SHARED) != null) {
                record.setShared(((Number) converter.get(SHARED)).intValue());
            }
            return record;
        }

        @Override
        public void entity2Storage(final ZipkinSpanRecord storageData, final Convert2Storage converter) {
            converter.accept(TRACE_ID, storageData.getTraceId());
            converter.accept(SPAN_ID, storageData.getSpanId());
            converter.accept(PARENT_ID, storageData.getParentId());
            converter.accept(NAME, storageData.getName());
            converter.accept(KIND, storageData.getKind());
            converter.accept(TIMESTAMP, storageData.getTimestamp());
            converter.accept(TIMESTAMP_MILLIS, storageData.getTimestampMillis());
            converter.accept(DURATION, storageData.getDuration());
            converter.accept(LOCAL_ENDPOINT_SERVICE_NAME, storageData.getLocalEndpointServiceName());
            converter.accept(LOCAL_ENDPOINT_IPV4, storageData.getLocalEndpointIPV4());
            converter.accept(LOCAL_ENDPOINT_IPV6, storageData.getLocalEndpointIPV6());
            if (storageData.getLocalEndpointPort() != 0) {
                converter.accept(LOCAL_ENDPOINT_PORT, storageData.getLocalEndpointPort());
            }
            converter.accept(REMOTE_ENDPOINT_SERVICE_NAME, storageData.getRemoteEndpointServiceName());
            converter.accept(REMOTE_ENDPOINT_IPV4, storageData.getRemoteEndpointIPV4());
            converter.accept(REMOTE_ENDPOINT_IPV6, storageData.getRemoteEndpointIPV6());
            if (storageData.getRemoteEndpointPort() != 0) {
                converter.accept(REMOTE_ENDPOINT_PORT, storageData.getRemoteEndpointPort());
            }
            if (storageData.getAnnotations() != null) {
                converter.accept(ANNOTATIONS, GSON.toJson(storageData.getAnnotations()));
            } else {
                converter.accept(ANNOTATIONS, Const.EMPTY_STRING);
            }
            if (storageData.getTags() != null) {
                converter.accept(TAGS, GSON.toJson(storageData.getTags()));
            } else {
                converter.accept(TAGS, Const.EMPTY_STRING);
            }
            converter.accept(QUERY, storageData.getQuery());
            if (storageData.getDebug() == BooleanUtils.booleanToValue(true)) {
                converter.accept(DEBUG, storageData.getDebug());
            }
            if (storageData.getShared() == BooleanUtils.booleanToValue(true)) {
                converter.accept(SHARED, storageData.getShared());
            }
        }
    }
}
