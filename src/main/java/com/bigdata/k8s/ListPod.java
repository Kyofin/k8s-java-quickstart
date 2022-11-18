package com.bigdata.k8s;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * When you use DefaultKubernetesClient, it will try to read the ~/.kube/config file in your home directory and load information required for authenticating with the Kubernetes API server.
 * You can override this configuration with the system property KUBECONFIG.
 * If you are using DefaultKubernetesClient from inside a Pod, it will load ~/.kube/config from the ServiceAccount volume mounted inside the Pod.
 */
public class ListPod {
    public static void main(String[] args) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {

            client.pods().inNamespace("bigdata-dev").list().getItems().forEach(
                    pod -> System.out.println(pod.getMetadata().getName())
            );

        } catch (KubernetesClientException ex) {
            // Handle exception
            ex.printStackTrace();
        }

    }
}
