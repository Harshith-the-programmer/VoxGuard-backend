package com.voiceclone.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
    exclude = {UserDetailsServiceAutoConfiguration.class}
)
public class VoiceCloneBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            VoiceCloneBackendApplication.class,
            args
        );
    }
}
