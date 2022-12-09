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
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@SpringBootTest(classes = K8sJavaQuickstartApplication.class)
class YamlApplicationTests {

    @Test
    void contextLoads() {
    }


    @Test
    void parse() throws FileNotFoundException {
        BufferedReader br = new BufferedReader(new FileReader(YamlApplicationTests.class.getClassLoader().getResource("parse.yaml").getPath()));
        Yaml yaml = new Yaml();
        Map<String, Object> objectMap = yaml.load(br);
        // 获取所有的key
        Set<String> keySet = objectMap.keySet();
        // 通过 key 获取value
        for (String key : keySet) {
            System.out.println(key + "\t : " + objectMap.get(key).toString());
        }

        System.out.println("=========获取单值==========");
        // 获取单值
        Object name = objectMap.get("name");
        System.out.println(name);

        System.out.println("=========获取数组类型==========");
        // 获取数组类型
        ArrayList<String> teamMember = (ArrayList<String>) objectMap.get("teamMember");
        for (String member : teamMember) {
            System.out.println(member);
        }

        System.out.println("=========获取复合对象==========");
        // 获取复合对象 , 注意类型为 LinkedHashMap
        LinkedHashMap<String, ArrayList<String>> hobbies = (LinkedHashMap<String, ArrayList<String>>) objectMap.get("hobbies");
        for (Map.Entry<String, ArrayList<String>> listEntry : hobbies.entrySet()) {
            System.out.println(listEntry.getKey() + " : " + listEntry.getValue());
        }

        BufferedReader br2 = new BufferedReader(new FileReader(YamlApplicationTests.class.getClassLoader().getResource("parse.yaml").getPath()));

        Person person = yaml.loadAs(br2, Person.class);
        System.out.println(person.getCompany());

    }
}
