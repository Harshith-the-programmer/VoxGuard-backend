package com.voiceclone.backend.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test/protected")
    public Map<String, String> protectedEndpoint(
            Authentication authentication) {

        return Map.of(
                "message", "JWT authentication is working",
                "user", authentication.getName()
        );
    }
}