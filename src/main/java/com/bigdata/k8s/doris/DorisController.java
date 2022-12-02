package com.bigdata.k8s.doris;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("doris")
public class DorisController {

    @Autowired
    InstallDorisService installDorisService;

    @RequestMapping(method = RequestMethod.GET,value = "/install")
    public String install() {
        installDorisService.handle();
        return "install...";
    }
}
