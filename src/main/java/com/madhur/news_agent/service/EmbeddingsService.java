package com.madhur.news_agent.service;

import com.madhur.news_agent.models.News;
import com.madhur.news_agent.repository.NewsRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingsService {

    private final NewsRepository repo;
    private final RestClient restClient;

    public EmbeddingsService(NewsRepository repo, RestClient.Builder builder) {
        this.repo = repo;
        this.restClient = builder.baseUrl("http://localhost:11434").build();
    }

    public int fill(int limit) {

        List<News> articles = repo.findByEmbeddingIsNull();

        int updated = 0;
        for (News a : articles) {
            if (updated >= limit) break;

            String text = buildText(a);
            if (text.isBlank()) continue;

            try {
                var vec = embed(text);
                a.setEmbedding(vec);
                repo.save(a);
                updated++;

                System.out.println("✅ Embedded: " + a.getTitle());
            } catch (Exception e) {
                System.out.println("❌ Failed: " + a.getId() + " => " + e.getMessage());
            }
        }
        return updated;
    }

    public List<Float> embed(String text) {

        Map<String, Object> body = Map.of(
                "model", "nomic-embed-text",
                "prompt", text
        );

        Map response = restClient.post()
                .uri("/api/embeddings")
                .body(body)
                .retrieve()
                .body(Map.class);

        List<Number> embedding = (List<Number>) response.get("embedding");
        return embedding.stream().map(Number::floatValue).toList();
    }

//    private String buildText(News a) {
//        String title = a.getTitle() == null ? "" : a.getTitle();
//        String desc = a.getDescription() == null ? "" : a.getDescription();
//        String content = a.getContent() == null ? "" : a.getContent();
//
//        return ("Title: " + title + "\nDescription: " + desc + "\nContent: " + content).trim();
//    }

    private String buildText(News a) {

        String title = a.getTitle() == null ? "" : a.getTitle();
        String desc = a.getDescription() == null ? "" : a.getDescription();
        String content = a.getContent() == null ? "" : a.getContent();

        // 🔥 prioritize important info
        String base = (title + ". " + desc).trim();

        // 🔥 limit content size
        int limit = 1200; // safe for nomic
        String shortContent = content.length() > limit
                ? content.substring(0, limit)
                : content;

        return (base + ". " + shortContent).trim();
    }

}
