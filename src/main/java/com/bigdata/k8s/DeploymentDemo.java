package com.bigdata.k8s;


import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.LogWatch;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DeploymentDemo {
    public static void main(String[] args) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            Deployment deployment = new DeploymentBuilder()
                    .withNewMetadata()
                    .withName("demo-deployment")
                    .addToLabels("app", "demo")
                    .endMetadata()
                    .withNewSpec()
                    .withReplicas(3)
                    .withNewSelector()
                    .addToMatchLabels("app", "demo")
                    .endSelector()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", "demo")
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName("demo")
                    .withImage("registry.mufankong.top/bigdata/httpd:2.4")
                    .addNewPort().withContainerPort(80).endPort()
                    .endContainer()
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();
            // 创建
            client.apps().deployments().inNamespace("default").createOrReplace(deployment);

            // deployment创建watch
            client.apps().deployments().watch(new Watcher<Deployment>() {
                @Override
                public void eventReceived(Action action, Deployment deployment) {
                    System.out.println("Watch event received " + action.name() + " " + deployment.getMetadata().getName());
                }

                @Override
                public void onClose(KubernetesClientException e) {
                    System.out.println("Watch gracefully closed");
                }
            });
            // 根据deploy 的label找podName
            Map<String, String> matchLabels = client.apps().deployments().inNamespace("default").withName("demo-deployment").get().getSpec().getSelector().getMatchLabels();
            for (Pod pod : client.pods().inNamespace("default").withLabels(matchLabels).list().getItems()) {
                String podName = pod.getMetadata().getName();
                System.out.println(podName);
                client.pods().inNamespace("default").withName(podName).waitUntilReady(5, TimeUnit.MINUTES);
                // 打印日志
                LogWatch watch = client.pods().inNamespace("default").withName(podName).watchLog(System.out);
                System.out.println(client.pods().inNamespace("default").withName(podName).getLog());
            }


            // 删除deployment
            client.apps().deployments().inNamespace("default").delete();
        } catch (KubernetesClientException ex) {
            // Handle exception
            ex.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
