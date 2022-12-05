package com.bigdata.k8s.doris;

import cn.hutool.core.io.FileUtil;
import com.bigdata.k8s.doris.config.DorisBeConfig;
import com.bigdata.k8s.doris.config.DorisClusterConfig;
import com.bigdata.k8s.doris.config.DorisFeConfig;
import com.bigdata.k8s.util.LoggerUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
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
        // custom logger统一日志生成到文件。。。
        Logger taskLogger = LoggerFactory.getLogger(LoggerUtils.buildTaskId(LoggerUtils.TASK_LOGGER_INFO_PREFIX,
                1,
                1,
                2));
        taskLogger.info("开始安装doris。。。。");
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            // 选择主机安装fe （fl001）
            List<String> feInstallHosts = Lists.newArrayList("fl001");
            feInstallHosts.forEach(new Consumer<String>() {
                @Override
                public void accept(String node) {
                    client.nodes().withName(node)
                            .edit()
                            .editMetadata()
                            .addToLabels(DORIS_FE_LABEL, DORIS_FE_LABEL_VALUE).addToLabels(CLUSTER_NAME_LABEL, clusterName)
                            .endMetadata()
                            .done();
                }
            });


            // 选择主机安装be（fl001,fl002,fl003）
            List<String> beInstallHosts = Lists.newArrayList("fl001", "fl002", "fl003");
            beInstallHosts.forEach(new Consumer<String>() {
                @Override
                public void accept(String node) {
                    client.nodes().withName(node)
                            .edit()
                            .editMetadata()
                            .addToLabels(DORIS_BE_LABEL, DORIS_BE_LABEL_VALUE).addToLabels(CLUSTER_NAME_LABEL, clusterName)
                            .endMetadata()
                            .done();
                }
            });

            // 用户调优参数
            // 服务端口选择
            DorisBeConfig dorisBEConfig = DorisBeConfig.builder()
                    .dockerImage("registry.mufankong.top/bigdata/doris-be:1.1.0")
                    .replicas(beInstallHosts.size())
                    .nodeSelectors(ImmutableMap.of(DORIS_BE_LABEL, DORIS_BE_LABEL_VALUE,CLUSTER_NAME_LABEL,clusterName))
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
                    .nodeSelectors(ImmutableMap.of(DORIS_FE_LABEL, DORIS_FE_LABEL_VALUE,CLUSTER_NAME_LABEL,clusterName))
                    .build();
            DorisClusterConfig dorisClusterConfig = DorisClusterConfig.builder()
                    .clusterName(clusterName)
                    .dorisBeConfig(dorisBEConfig)
                    .dorisFeConfig(dorisFEConfig).build();

            // todo 存储安装清单到数据库

            // 根据安装清单生成k8s资源文件
            renderDorisConfig("doris-configmap.yaml", "doris", dorisClusterConfig);
            renderDorisConfig("doris-script-confimap.yaml", "doris", dorisClusterConfig);
            renderDorisConfig("FE-statefulset.yaml", "doris", dorisClusterConfig);
            renderDorisConfig("BE-statefulset.yaml", "doris", dorisClusterConfig);
            // 生成be的properties文件内容
            // 生成fe的properties文件内容
            // 生成fe启动脚本的内容
            // 生成be启动脚本的内容
            // 生成configmap

            // 生成fe的statefulset
            // 生成be的statefulset

            // 按顺序启动fe
            // 为节点打上第一个fe的label
            // 提交fe的statefulset资源到k8s
            // 轮询监听fe的pod状态并打印pod的日志。一旦成功则fe的启动成功，继续。一旦失败则中断
            // 按顺序启动be，当第一个成功启动后才继续（scale up），一旦检查到失败即中断


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
