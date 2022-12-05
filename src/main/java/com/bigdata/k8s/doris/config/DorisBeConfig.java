package com.bigdata.k8s.doris.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DorisBeConfig {
    private int bePort;
    private int webserverPort;
    private int heartBeatPort;
    private int brpcPort;
    private String dockerImage;


}
