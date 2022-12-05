package com.example.k8sjavaquickstart;

import com.bigdata.k8s.K8sJavaQuickstartApplication;
import com.bigdata.k8s.doris.config.DorisBeConfig;
import com.bigdata.k8s.doris.config.DorisClusterConfig;
import com.bigdata.k8s.doris.config.DorisFeConfig;
import org.beetl.core.Configuration;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@SpringBootTest(classes = K8sJavaQuickstartApplication.class)
class BeetApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void beetString() throws IOException {
        ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader("templates");
        Configuration cfg = Configuration.defaultConfiguration();
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        Template t = gt.getTemplate("/hello.btl");
        t.binding("name", "beetl");
        String str = t.render();
        System.out.println(str);
    }

    @Test
    void beetObj() throws IOException {
        ClasspathResourceLoader resourceLoader = new ClasspathResourceLoader("templates");
        Configuration cfg = Configuration.defaultConfiguration();
        GroupTemplate gt = new GroupTemplate(resourceLoader, cfg);
        Template t = gt.getTemplate("/doris.btl");
        DorisBeConfig dorisBEConfig = DorisBeConfig.builder()
                .dockerImage("registry.mufankong.top/bigdata/doris-be:1.1.0")
                .bePort(9060)
                .heartBeatPort(9050)
                .webserverPort(8041)
                .brpcPort(8060).build();
        DorisFeConfig dorisFEConfig = DorisFeConfig.builder()
                .dockerImage("registry.mufankong.top/bigdata/doris-fe:1.1.0")
                .httpPort(8030)
                .rpcPort(9020)
                .queryPort(9030)
                .editLogPort(9010).build();
        DorisClusterConfig dorisClusterConfig = DorisClusterConfig.builder()
                .clusterName("udh")
                .dorisBeConfig(dorisBEConfig)
                .dorisFeConfig(dorisFEConfig).build();
        t.binding("doris", dorisClusterConfig);
        String str = t.render();
        System.out.println(str);
    }
}
