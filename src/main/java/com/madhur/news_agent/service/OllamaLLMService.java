package com.madhur.news_agent.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class OllamaLLMService {

    private final RestClient restClient;

    public OllamaLLMService(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://localhost:11434").build();
    }

    public String generateJson(String prompt) {

        Map<String, Object> body = Map.of(
                "model", "mistral:7b",
                "prompt", prompt,
                "stream", false,
                "format", "json",
                "options", Map.of(
                        "temperature", 0.3,
                        "num_predict", 260,
                        "top_p", 0.9
                )
        );

        return callOllama(body);
    }

    public String generateText(String prompt) {

        Map<String, Object> body = Map.of(
                "model", "mistral:7b",
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "temperature", 0.7,
                        "num_predict", 260,
                        "top_p", 0.9
                )
        );

        return callOllama(body);
    }

    private String callOllama(Map<String, Object> body) {

        Map response = restClient.post()
                .uri("/api/generate")
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response != null && response.get("response") != null) {
            return response.get("response").toString().trim();
        }
        return "";
    }
}
