package com.bigdata.k8s;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeAddress;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

import java.util.HashMap;
import java.util.Map;

public class NodeDemo {
    public static void main(String[] args) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            // 打印每个节点的信息
            for (Node item : client.nodes().list().getItems()) {
                System.out.println("=============开始打印" + item.getMetadata().getName() + "============");
                for (NodeAddress address : item.getStatus().getAddresses()) {
                    System.out.println(address.getType() + ":  " + address.getAddress());
                }
                System.out.println("操作系统：" + item.getStatus().getNodeInfo().getOperatingSystem());
                for (Map.Entry<String, String> stringEntry : item.getMetadata().getLabels().entrySet()) {
                    System.out.println(stringEntry.getKey() + ":" + stringEntry.getValue());
                }
                System.out.println("=============结束打印" + item.getMetadata().getName() + "============");
            }
            // 添加label
            client.nodes().withName("fl001")
                    .edit()
                    .editMetadata()
                    .addToLabels("my-hdfs-dn", "true").addToLabels("cluster-name", "dev")
                    .endMetadata()
                    .done();
            // 检查label
            System.out.println(client.nodes().withName("fl001").get().getMetadata().getLabels().get("my-hdfs-dn").equals("true"));
            System.out.println(client.nodes().withName("fl001").get().getMetadata().getLabels().get("cluster-name").equals("dev"));
            // 移除lable
            client.nodes().withName("fl001")
                    .edit()
                    .editMetadata()
                    .removeFromLabels("my-hdfs-dn")
                    .removeFromLabels("cluster-name")
                    .endMetadata()
                    .done();

        } catch (KubernetesClientException ex) {
            // Handle exception
            ex.printStackTrace();
        }
    }
}
