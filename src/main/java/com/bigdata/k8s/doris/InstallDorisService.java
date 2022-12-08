package com.bigdata.k8s.doris;

import cn.hutool.core.io.FileUtil;
import com.bigdata.k8s.YamlResource;
import com.bigdata.k8s.doris.config.DorisBeConfig;
import com.bigdata.k8s.doris.config.DorisClusterConfig;
import com.bigdata.k8s.doris.config.DorisFeConfig;
import com.bigdata.k8s.util.LoggerUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apps.DoneableStatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class InstallDorisService {

    public static final String RENDER_OUTPUT_PATH = "/Users/huzekang/study/k8s-java-quickstart/render_out/";
    public static final String DORIS_FE_LABEL = "doris-fe";
    public static final String DORIS_FE_LABEL_VALUE = "true";
    public static final String CLUSTER_NAME_LABEL = "cluster-name";
    public static final String DORIS_BE_LABEL = "doris-be";
    public static final String DORIS_BE_LABEL_VALUE = "true";

    public void handle() {
        String clusterName = "pingdata";
        String namespace = clusterName;
        // custom logger统一日志生成到文件。。。
        Logger taskLogger = LoggerFactory.getLogger(LoggerUtils.buildTaskId(LoggerUtils.TASK_LOGGER_INFO_PREFIX,
                1,
                1,
                2));
        taskLogger.info("开始安装doris。。。。");
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            List<String> feInstallHosts = Lists.newArrayList("fl001");
            List<String> beInstallHosts = Lists.newArrayList("fl001", "fl002", "fl003");

            // 用户调优参数
            // 服务端口选择
            DorisBeConfig dorisBEConfig = DorisBeConfig.builder()
                    .dockerImage("registry.mufankong.top/bigdata/doris-be:1.1.0")
                    .replicas(beInstallHosts.size())
                    .nodeSelectors(ImmutableMap.of(DORIS_BE_LABEL, DORIS_BE_LABEL_VALUE, CLUSTER_NAME_LABEL, clusterName))
                    .bePort(9060)
                    .heartBeatPort(9050)
                    .webserverPort(8041)
                    .brpcPort(8060).build();
            DorisFeConfig dorisFEConfig = DorisFeConfig.builder()
                    .dockerImage("registry.mufankong.top/bigdata/doris-fe:1.1.0")
                    .httpPort(8030)
                    .rpcPort(9020)
                    .queryPort(9030)
                    .editLogPort(9010)
                    .registerFeIp(feInstallHosts.get(0))
                    .nodeSelectors(ImmutableMap.of(DORIS_FE_LABEL, DORIS_FE_LABEL_VALUE, CLUSTER_NAME_LABEL, clusterName))
                    .build();
            DorisClusterConfig dorisClusterConfig = DorisClusterConfig.builder()
                    .clusterName(clusterName)
                    .dorisBeConfig(dorisBEConfig)
                    .dorisFeConfig(dorisFEConfig).build();

            // todo 存储安装清单到数据库

            // 根据安装清单生成k8s资源文件
            renderDorisConfig("FE-statefulset.yaml", "doris", dorisClusterConfig);
            renderDorisConfig("BE-statefulset.yaml", "doris", dorisClusterConfig);
            renderDorisConfig("doris-configmap.yaml", "doris", dorisClusterConfig);
            renderDorisConfig("doris-script-confimap.yaml", "doris", dorisClusterConfig);

            // 创建namespace
            Namespace ns = new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build();
            client.namespaces().createOrReplace(ns);

            // 提交fe be的配置的configmap
            ConfigMap configMap = client.configMaps().load(new File(RENDER_OUTPUT_PATH + "/doris/doris-configmap.yaml")).get();
            client.configMaps().inNamespace(namespace).createOrReplace(configMap);

            // 提交fe be的脚本的configmap
            ConfigMap scriptConfigmap = client.configMaps().load(new File(RENDER_OUTPUT_PATH + "/doris/doris-script-confimap.yaml")).get();
            client.configMaps().inNamespace(namespace).createOrReplace(scriptConfigmap);

            // 提交fe的statefulset资源到k8s
            StatefulSet feStatefulSet = client.apps().statefulSets()
                    .load(new File(RENDER_OUTPUT_PATH + "/doris/FE-statefulset.yaml"))
                    .get();

             client.apps().statefulSets().inNamespace(namespace).createOrReplace(feStatefulSet);
            // 按顺序启动fe
            feInstallHosts.forEach(node -> {
                // 为节点打上第一个fe的label
                client.nodes().withName(node)
                        .edit()
                        .editMetadata()
                        .addToLabels(DORIS_FE_LABEL, DORIS_FE_LABEL_VALUE).addToLabels(CLUSTER_NAME_LABEL, clusterName)
                        .endMetadata()
                        .done();
                // 轮询监听fe的statefulset状态变为ready。
                try {
                    System.out.println("waiting for fe statefulSet is ready within 600s");
                    StatefulSet readyFeStatefulSet = client.resource(feStatefulSet).inNamespace(clusterName).waitUntilReady(600, TimeUnit.SECONDS);
                    Integer readyReplicas = readyFeStatefulSet.getStatus().getReadyReplicas();
                    System.out.println("fe statefulSet is ready now.... , ready实例数："+readyReplicas);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            // 提交be的statefulset资源到k8s
            StatefulSet beStatefulSet = client.apps().statefulSets()
                    .load(new File(RENDER_OUTPUT_PATH + "/doris/BE-statefulset.yaml"))
                    .get();
            client.apps().statefulSets().inNamespace(namespace).createOrReplace(beStatefulSet);

            String beStatefulSetName = beStatefulSet.getMetadata().getName();
            // 按顺序启动be，当第一个成功启动后才继续（scale up）
            for (int i = 0; i < beInstallHosts.size(); i++) {
                String node = beInstallHosts.get(i);
                int count = 1 + i;
                System.out.println("scale up be statefulSet to:"+count);
                client.apps().statefulSets().inNamespace(namespace).withName(beStatefulSetName).scale(count);

                client.nodes().withName(node)
                        .edit()
                        .editMetadata()
                        .addToLabels(DORIS_BE_LABEL, DORIS_BE_LABEL_VALUE).addToLabels(CLUSTER_NAME_LABEL, clusterName)
                        .endMetadata()
                        .done();
                // 轮询监听be的statefulset状态变为ready。
                try {
                    System.out.println("waiting for be statefulSet is ready within 600s");
                    StatefulSet readyBeStatefulSet = client.resource(beStatefulSet).inNamespace(clusterName).waitUntilReady(600, TimeUnit.SECONDS);
                    Integer readyReplicas = readyBeStatefulSet.getStatus().getReadyReplicas();
                    System.out.println("be statefulSet is ready now.... , ready实例数："+readyReplicas);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        } catch (KubernetesClientException ex) {
            // Handle exception
            ex.printStackTrace();
        }

        taskLogger.info("完成安装doris。。。。");
    }

    public void renderDorisConfig(String templateName, String varName, Object renderObj) {

        try {
            ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader("templates/doris");
            Configuration cfg = Configuration.defaultConfiguration();
            GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
            Template t = gt.getTemplate(templateName);
            t.binding(varName, renderObj);
//            String str = t.render();
//            System.out.println(str);
            String path = RENDER_OUTPUT_PATH + "/doris/";
            if (!FileUtil.exist(path)) {
                FileUtil.mkdir(path);
            }
            t.renderTo(new FileOutputStream(path + templateName));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
