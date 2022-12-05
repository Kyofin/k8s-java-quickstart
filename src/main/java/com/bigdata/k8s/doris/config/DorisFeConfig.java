package com.bigdata.k8s.doris.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DorisFeConfig {
    private int httpPort;
    private int rpcPort;
    private int queryPort;
    private int editLogPort;
    private String dockerImage;

}
