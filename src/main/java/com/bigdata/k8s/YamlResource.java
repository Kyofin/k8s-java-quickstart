package com.bigdata.k8s;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class YamlResource {
    public static void main(String[] args) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {

            Service service = client.services()
                    .load(YamlResource.class.getResourceAsStream("/service.yaml"))
                    .get();

            //create
            client.services().inNamespace("default").createOrReplace(service);
            // Get
            Service deploy = client.services()
                    .inNamespace("default")
                    .withName("my-service")
                    .get();

            System.out.println(deploy.toString());

            // Update, adding dummy annotation
            Service updatedDeploy = client.services()
                    .inNamespace("default")
                    .withName("my-service")
                    .edit()
                    .editMetadata().addToAnnotations("foo", "bar").endMetadata()
                    .done();

            // Deletion
            Boolean isDeleted = client.services()
                    .inNamespace("default")
                    .withName("my-service")
                    .delete();
            System.out.println("删除是否成功："+isDeleted);



        } catch (KubernetesClientException ex) {
            // Handle exception
            ex.printStackTrace();
        }
    }
}
