package com.dockerforjavadevelopers.hello.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public String index() {
        return "Welcome to Java App2 API (uses java-app1 from GitHub Package). Try /api/hello or /swagger-ui.html";
    }
}
