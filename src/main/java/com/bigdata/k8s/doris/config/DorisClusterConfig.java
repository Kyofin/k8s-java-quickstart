package com.bigdata.k8s.doris.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DorisClusterConfig {
    private String clusterName;
    private DorisBeConfig dorisBeConfig;
    private DorisFeConfig dorisFeConfig;

}
