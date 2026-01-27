package com.madhur.news_agent.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OllamaEmbeddingService {

    private final RestClient restClient;
    public OllamaEmbeddingService(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://localhost:11434").build();
    }

    @SuppressWarnings("unchecked")
    public List<Float> embed(String text) {

        Map<String, Object> body = Map.of(
                "model", "nomic-embed-text",
                "prompt", text
        );

        Map<String, Object> response = restClient.post()
                .uri("/api/embeddings")
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Number> embedding = (List<Number>) response.get("embedding");
        return embedding.stream().map(Number::floatValue).toList();
    }
}
