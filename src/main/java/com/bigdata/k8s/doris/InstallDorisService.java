package com.bigdata.k8s.doris;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.bigdata.k8s.util.LoggerUtils;
import com.google.common.collect.Lists;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class InstallDorisService {
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
            Lists.newArrayList("fl001").forEach(new Consumer<String>() {
                @Override
                public void accept(String node) {
                    client.nodes().withName(node)
                            .edit()
                            .editMetadata()
                            .addToLabels("doris-fe", "true").addToLabels("cluster-name", clusterName)
                            .endMetadata()
                            .done();
                }
            });


            // 选择主机安装be（fl001,fl002,fl003）
            Lists.newArrayList("fl001","fl002","fl003").forEach(new Consumer<String>() {
                @Override
                public void accept(String node) {
                    client.nodes().withName(node)
                            .edit()
                            .editMetadata()
                            .addToLabels("doris-be", "true").addToLabels("cluster-name", clusterName)
                            .endMetadata()
                            .done();
                }
            });

            // 用户调优参数
            // 服务端口选择
            // 存储安装清单到数据库
            // 根据安装清单生成k8s资源文件
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

    public void render() throws URISyntaxException, IOException, TemplateException {
        // 创建配置类
        Configuration configuration = new Configuration(Configuration.getVersion());
        // 设置模板路径 toURI()防止路径出现空格
        String classpath = this.getClass().getResource("/").toURI().getPath();
        configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/doris"));
        // 设置字符集
        configuration.setDefaultEncoding("utf-8");
        // 加载模板
        Template template = configuration.getTemplate("demo1.ftl");
        // 数据模型
        Map<String, Object> map = new HashMap<>();
        map.put("name", "静态化测试");
        // 静态化
        String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        // 打印静态化内容
        System.out.println(content);
        InputStream inputStream = IoUtil.toStream(content.getBytes(StandardCharsets.UTF_8));
        // 输出文件
        FileOutputStream fileOutputStream = new FileOutputStream(new File("demo1.html"));
        IoUtil.copy(inputStream, fileOutputStream);
    }
}
