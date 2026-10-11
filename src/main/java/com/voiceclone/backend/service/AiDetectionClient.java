
package com.voiceclone.backend.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class AiDetectionClient {

    private final RestClient restClient;

    public AiDetectionClient(
            @Value("${ai.service-url:http://127.0.0.1:8000}")
            String serviceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                .build();
    }

    public Map<String, Object> detect(Path audioPath) {

        if (audioPath == null
                || !Files.isRegularFile(audioPath)
                || !Files.isReadable(audioPath)) {
            throw new IllegalArgumentException(
                    "Stored audio file is unavailable.");
        }

        MultiValueMap<String, Object> parts =
                new LinkedMultiValueMap<>();

        parts.add("file", new FileSystemResource(audioPath));

        Map<String, Object> response = restClient.post()
                .uri("/api/audio/detect")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .body(new ParameterizedTypeReference<
                        Map<String, Object>>() {});

        if (response == null
                || !(response.get("result") instanceof Map<?, ?> rawResult)) {
            throw new IllegalStateException(
                    "AI service returned an invalid response.");
        }

        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<?, ?> entry : rawResult.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }

        return result;
    }
}
