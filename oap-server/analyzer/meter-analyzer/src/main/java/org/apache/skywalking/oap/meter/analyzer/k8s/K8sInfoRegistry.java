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

package org.apache.skywalking.oap.meter.analyzer.k8s;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1LoadBalancerIngress;
import io.kubernetes.client.openapi.models.V1LoadBalancerStatus;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.openapi.models.V1ServiceSpec;
import io.kubernetes.client.openapi.models.V1ServiceStatus;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Pod;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;
import org.apache.skywalking.oap.server.library.util.StringUtil;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

@Slf4j
public class K8sInfoRegistry {

    private final static K8sInfoRegistry INSTANCE = new K8sInfoRegistry();
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final Map<String/* podName.namespace */, V1Pod> namePodMap = new ConcurrentHashMap<>();
    protected final Map<String/* serviceName.namespace  */, V1Service> nameServiceMap = new ConcurrentHashMap<>();
    private final Map<String/* podName.namespace */, String /* serviceName.namespace */> podServiceMap = new ConcurrentHashMap<>();
    private final Map<String/* podIP */, String /* podName.namespace */> ipPodMap = new ConcurrentHashMap<>();
    private final Map<String/* serviceIP */, String /* serviceName.namespace */> ipServiceMap = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private static final String SEPARATOR = ".";

    public static K8sInfoRegistry getInstance() {
        return INSTANCE;
    }

    private void init() {
        executor = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .setNameFormat("K8sInfoRegistry-%d")
                .setDaemon(true)
                .build()
        );
    }

    @SneakyThrows
    public void start() {
        if (isStarted.compareAndSet(false, true)) {
            init();
            final ApiClient apiClient = Config.defaultClient();
            apiClient.setHttpClient(apiClient.getHttpClient()
                                             .newBuilder()
                                             .readTimeout(0, TimeUnit.SECONDS)
                                             .build());
            Configuration.setDefaultApiClient(apiClient);

            final CoreV1Api coreV1Api = new CoreV1Api();
            final SharedInformerFactory factory = new SharedInformerFactory(executor);
            listenServiceEvents(coreV1Api, factory);
            listenPodEvents(coreV1Api, factory);
            factory.startAllRegisteredInformers();
        }
    }

    private void listenServiceEvents(final CoreV1Api coreV1Api, final SharedInformerFactory factory) {
        factory.sharedIndexInformerFor(
            params -> coreV1Api.listServiceForAllNamespacesCall(
                null,
                null,
                null,
                null,
                null,
                null,
                params.resourceVersion,
                null,
                params.timeoutSeconds,
                params.watch,
                null
            ),
            V1Service.class,
            V1ServiceList.class
        ).addEventHandler(new ResourceEventHandler<V1Service>() {
            @Override
            public void onAdd(final V1Service service) {
                addService(service);
            }

            @Override
            public void onUpdate(final V1Service oldService, final V1Service newService) {
                addService(newService);
            }

            @Override
            public void onDelete(final V1Service service, final boolean deletedFinalStateUnknown) {
                removeService(service);
            }
        });
    }

    private void listenPodEvents(final CoreV1Api coreV1Api, final SharedInformerFactory factory) {
        factory.sharedIndexInformerFor(
            params -> coreV1Api.listPodForAllNamespacesCall(
                null,
                null,
                null,
                null,
                null,
                null,
                params.resourceVersion,
                null,
                params.timeoutSeconds,
                params.watch,
                null
            ),
            V1Pod.class,
            V1PodList.class
        ).addEventHandler(new ResourceEventHandler<V1Pod>() {
            @Override
            public void onAdd(final V1Pod pod) {
                addPod(pod);
            }

            @Override
            public void onUpdate(final V1Pod oldPod, final V1Pod newPod) {
                addPod(newPod);
            }

            @Override
            public void onDelete(final V1Pod pod, final boolean deletedFinalStateUnknown) {
                removePod(pod);
            }
        });
    }

    protected void addService(final V1Service service) {
        ofNullable(service.getMetadata()).ifPresent(
            metadata -> nameServiceMap.put(metadata.getName() + SEPARATOR + metadata.getNamespace(), service)
        );
        recompose();
    }

    protected void removeService(final V1Service service) {
        ofNullable(service.getMetadata()).ifPresent(
            metadata -> nameServiceMap.remove(metadata.getName() + SEPARATOR + metadata.getNamespace())
        );
        ofNullable(service.getStatus()).map(V1ServiceStatus::getLoadBalancer).filter(Objects::nonNull)
            .map(V1LoadBalancerStatus::getIngress).filter(CollectionUtils::isNotEmpty)
            .ifPresent(l -> l.stream().filter(i -> StringUtil.isNotEmpty(i.getIp()))
                    .forEach(i -> ipServiceMap.remove(i.getIp())));
        ofNullable(service.getSpec()).map(V1ServiceSpec::getClusterIPs).filter(CollectionUtils::isNotEmpty)
            .ifPresent(l -> l.stream().filter(StringUtil::isNotEmpty).forEach(ipServiceMap::remove));
        recompose();
    }

    protected void addPod(final V1Pod pod) {
        ofNullable(pod.getMetadata()).ifPresent(
            metadata -> namePodMap.put(metadata.getName() + SEPARATOR + metadata.getNamespace(), pod));

        recompose();
    }

    protected void removePod(final V1Pod pod) {
        ofNullable(pod.getMetadata()).ifPresent(
            metadata -> namePodMap.remove(metadata.getName() + SEPARATOR + metadata.getNamespace()));

        ofNullable(pod.getMetadata()).ifPresent(
            metadata -> podServiceMap.remove(metadata.getName() + SEPARATOR + metadata.getNamespace()));

        ofNullable(pod.getStatus()).filter(s -> StringUtil.isNotEmpty(s.getPodIP())).ifPresent(
            status -> ipPodMap.remove(status.getPodIP()));
    }

    private void recompose() {
        namePodMap.forEach((podName, pod) -> {
            if (!isNull(pod.getMetadata())) {
                ofNullable(pod.getStatus()).filter(s -> StringUtil.isNotEmpty(s.getPodIP())).ifPresent(
                        status -> ipPodMap.put(status.getPodIP(), podName));
            }

            nameServiceMap.forEach((serviceName, service) -> {
                if (isNull(pod.getMetadata()) || isNull(service.getMetadata()) || isNull(service.getSpec())) {
                    return;
                }

                Map<String, String> selector = service.getSpec().getSelector();
                Map<String, String> labels = pod.getMetadata().getLabels();

                if (isNull(labels) || isNull(selector)) {
                    return;
                }

                String podNamespace = pod.getMetadata().getNamespace();
                String serviceNamespace = service.getMetadata().getNamespace();

                if (Strings.isNullOrEmpty(podNamespace) || Strings.isNullOrEmpty(
                    serviceNamespace) || !podNamespace.equals(serviceNamespace)) {
                    return;
                }

                if (hasIntersection(selector.entrySet(), labels.entrySet())) {
                    podServiceMap.put(podName, serviceName);
                }
            });
        });
        nameServiceMap.forEach((serviceName, service) -> {
            if (!isNull(service.getSpec()) && CollectionUtils.isNotEmpty(service.getSpec().getClusterIPs())) {
                for (String clusterIP : service.getSpec().getClusterIPs()) {
                    ipServiceMap.put(clusterIP, serviceName);
                }
            }
            if (!isNull(service.getStatus()) && !isNull(service.getStatus().getLoadBalancer())
                && CollectionUtils.isNotEmpty(service.getStatus().getLoadBalancer().getIngress())) {
                for (V1LoadBalancerIngress loadBalancerIngress : service.getStatus().getLoadBalancer().getIngress()) {
                    if (StringUtil.isNotEmpty(loadBalancerIngress.getIp())) {
                        ipServiceMap.put(loadBalancerIngress.getIp(), serviceName);
                    }
                }
            }
        });
    }

    public String findServiceName(String namespace, String podName) {
        return this.podServiceMap.get(podName + SEPARATOR + namespace);
    }

    public String findPodByIP(String ip) {
        return this.ipPodMap.get(ip);
    }

    public String findServiceByIP(String ip) {
        return this.ipServiceMap.get(ip);
    }

    private boolean hasIntersection(Collection<?> o, Collection<?> c) {
        Objects.requireNonNull(o);
        Objects.requireNonNull(c);
        for (final Object value : o) {
            if (!c.contains(value)) {
                return false;
            }
        }
        return true;
    }
}
