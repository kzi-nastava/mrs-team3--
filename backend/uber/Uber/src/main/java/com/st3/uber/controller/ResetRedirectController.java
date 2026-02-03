package com.st3.uber.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ResetRedirectController {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @GetMapping("/reset-password")
    public String redirect(@RequestParam String token) {
        return "redirect:" + frontendUrl + "/reset-password?token=" + token;
    }
}
