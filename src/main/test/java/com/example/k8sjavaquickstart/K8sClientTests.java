package com.example.k8sjavaquickstart;

import com.bigdata.k8s.K8sJavaQuickstartApplication;
import com.bigdata.k8s.doris.config.DorisBeConfig;
import com.bigdata.k8s.doris.config.DorisClusterConfig;
import com.bigdata.k8s.doris.config.DorisFeConfig;
import com.google.common.collect.Lists;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.bigdata.k8s.doris.InstallDorisService.*;

@SpringBootTest(classes = K8sJavaQuickstartApplication.class)
class K8sClientTests {

    @Test
    void contextLoads() {
    }

    @Test
    void createNamespace() {
        KubernetesClient client = new DefaultKubernetesClient();
        Namespace ns = new NamespaceBuilder().withNewMetadata().withName("java-k8s").addToLabels("hello", "world").endMetadata()
                .build();
        client.namespaces().createOrReplace(ns);
        client.namespaces().delete(ns);
    }

    @Test
    void removeLabel() throws IOException {
        KubernetesClient client = new DefaultKubernetesClient();
        List<String> beInstallHosts = Lists.newArrayList("fl001", "fl002", "fl003");
        beInstallHosts.forEach(new Consumer<String>() {
            @Override
            public void accept(String node) {
                client.nodes().withName(node)
                        .edit()
                        .editMetadata()
                        .removeFromLabels(DORIS_BE_LABEL)
                        .removeFromLabels(DORIS_FE_LABEL)
                        .removeFromLabels(CLUSTER_NAME_LABEL)
                        .endMetadata()
                        .done();
            }
        });
    }

    @Test
    void waitForReady() throws InterruptedException {
        KubernetesClient client = new DefaultKubernetesClient();
        StatefulSet ss1 = new StatefulSetBuilder()
                .withNewMetadata().withName("ss1").endMetadata()
                .withNewSpec()
                .withReplicas(2)
                .withNewSelector().withMatchLabels(Collections.singletonMap("app", "nginx")).endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", "nginx")
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName("nginx")
                .withImage("registry.mufankong.top/bigdata/nginx:1.7.9")
                .addNewPort()
                .withContainerPort(80)
                .withName("web")
                .endPort()
//                .addNewVolumeMount()
//                .withName("www")
//                .withMountPath("/usr/share/nginx/html")
//                .endVolumeMount()
                .endContainer()
                .endSpec()
                .endTemplate()
//                .addNewVolumeClaimTemplate()
//                .withNewMetadata()
//                .withName("www")
//                .endMetadata()
//                .withNewSpec()
//                .addToAccessModes("ReadWriteOnce")
//                .withNewResources()
//                .withRequests(Collections.singletonMap("storage", new Quantity("1Gi")))
//                .endResources()
//                .endSpec()
//                .endVolumeClaimTemplate()
                .endSpec()
                .build();
        client.apps().statefulSets().inNamespace("default").delete(ss1);
        client.apps().statefulSets().inNamespace("default").create(ss1);
        // 等待ready
        StatefulSet statefulSet = client.resource(ss1).inNamespace("default").waitUntilReady(60, TimeUnit.SECONDS);
        Integer readyReplicas = statefulSet.getStatus().getReadyReplicas();
        System.out.println("nginx is ready....，ready实例数："+readyReplicas);
    }


}
