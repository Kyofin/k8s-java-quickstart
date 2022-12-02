package com.example.k8sjavaquickstart;

import cn.hutool.core.io.IoUtil;
import com.bigdata.k8s.K8sJavaQuickstartApplication;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import sun.misc.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = K8sJavaQuickstartApplication.class)
class K8sJavaQuickstartApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void free() throws IOException, URISyntaxException, TemplateException {
        // 创建配置类
        Configuration configuration = new Configuration(Configuration.getVersion());
        // 设置模板路径 toURI()防止路径出现空格
        String classpath = this.getClass().getResource("/").toURI().getPath();
        configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
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
