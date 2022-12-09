package com.example.k8sjavaquickstart;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class Person {
    private String name;
    private String phone;
    private String company;
    private String shell;
    private List<String> teamMember;
    private LinkedHashMap<String, List<String>> hobbies;
    private List<String> workingSkills;

}
