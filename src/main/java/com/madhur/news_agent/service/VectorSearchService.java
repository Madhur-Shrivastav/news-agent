package com.madhur.news_agent.service;

import com.madhur.news_agent.dto.Citation;
import org.bson.Document;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VectorSearchService {

    private final MongoTemplate mongoTemplate;

    public VectorSearchService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    private final RestTemplate restTemplate = new RestTemplate();

    // =========================================
    // FASTAPI BATCH CALL
    // =========================================

    public List<Map<String, Object>> getBatchProbabilities(List<String> texts) {

        String url = "http://127.0.0.1:8000/predict_batch";

        List<String> trimmedTexts = texts.stream()
                .map(t -> t == null ? "" : t)
                .toList();

        Map<String, Object> request = new HashMap<>();
        request.put("texts", trimmedTexts);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(request, headers);

        try {

            ResponseEntity<List<Map<String, Object>>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                    );

            List<Map<String, Object>> body = response.getBody();

            if (body == null || body.size() != trimmedTexts.size()) {
                throw new RuntimeException("Invalid response from FastAPI");
            }

            return body;

        } catch (Exception e) {

            System.out.println("❌ Batch call failed: " + e.getMessage());

            return trimmedTexts.stream()
                    .map(t -> {
                        Map<String, Object> fallback = new HashMap<>();

                        fallback.put("fake_probability", 0.5);
                        fallback.put("real_probability", 0.5);
                        fallback.put("prediction", "UNKNOWN");

                        return fallback;
                    })
                    .toList();
        }
    }

    public List<Citation> searchRaw(
            List<Float> queryVector,
            int limit,
            List<String> entities,
            List<String> keywords
    ) {

        List<String> normalizedEntities = entities == null
                ? List.of()
                : entities.stream()
                .map(e -> e.toLowerCase().trim())
                .distinct()
                .toList();

        List<String> normalizedKeywords = keywords == null
                ? List.of()
                : keywords.stream()
                .map(k -> k.toLowerCase().trim())
                .distinct()
                .toList();

        Document vectorStageBody = new Document("index", "vector_index_2")
                .append("path", "embedding")
                .append("queryVector", queryVector)
                .append("numCandidates", 500)
                .append("limit", limit);

        List<Document> conditions = new ArrayList<>();

        if (!normalizedEntities.isEmpty()) {

            conditions.add(
                    new Document(
                            "entities",
                            new Document("$in", normalizedEntities)
                    )
            );
        }

        if (!normalizedKeywords.isEmpty()) {

            conditions.add(
                    new Document(
                            "keywords",
                            new Document("$in", normalizedKeywords)
                    )
            );
        }

        if (!conditions.isEmpty()) {

            Document filter;

            if (conditions.size() == 1) {
                filter = conditions.get(0);
            } else {
                filter = new Document("$and", conditions);
            }

            vectorStageBody.append("filter", filter);
        }

        Document vectorStage =
                new Document("$vectorSearch", vectorStageBody);

        Document projectStage = new Document(
                "$project",
                new Document("title", 1)
                        .append("description", 1)
                        .append("content", 1)
                        .append("url", 1)
                        .append("publishedAt", 1)
                        .append("sourceName", 1)
                        .append("entities", 1)
                        .append("keywords", 1)
                        .append(
                                "score",
                                new Document("$meta", "vectorSearchScore")
                        )
        );

        Aggregation agg = Aggregation.newAggregation(
                context -> vectorStage,
                context -> projectStage
        );

        List<Document> results = mongoTemplate
                .aggregate(
                        agg,
                        "news_articles",
                        Document.class
                )
                .getMappedResults();


        if (results.isEmpty()) {

            Document fallbackStage = new Document(
                    "$vectorSearch",
                    new Document("index", "vector_index_2")
                            .append("path", "embedding")
                            .append("queryVector", queryVector)
                            .append("numCandidates", 500)
                            .append("limit", limit)
            );

            Aggregation fallbackAgg = Aggregation.newAggregation(
                    context -> fallbackStage,
                    context -> projectStage
            );

            results = mongoTemplate
                    .aggregate(
                            fallbackAgg,
                            "news_articles",
                            Document.class
                    )
                    .getMappedResults();
        }

        // =========================================
        // CONTENT EXTRACTION
        // =========================================

        List<String> texts = results.stream()
                .map(doc -> {
                    String content = doc.getString("content");

                    return (content == null || content.isBlank())
                            ? ""
                            : content;
                })
                .toList();

        // =========================================
        // FASTAPI PREDICTIONS
        // =========================================

        List<Map<String, Object>> predictions =
                getBatchProbabilities(texts);

        if (predictions == null ||
                predictions.size() != results.size()) {

            throw new RuntimeException(
                    "Mismatch between input and predictions"
            );
        }

        // =========================================
        // DTO MAPPING
        // =========================================

        List<Citation> responses = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {

            Document doc = results.get(i);

            Map<String, Object> probability = predictions.get(i);

            double fakeProbability =
                    ((Number) probability.get("fake_probability"))
                            .doubleValue();

            double realProbability =
                    ((Number) probability.get("real_probability"))
                            .doubleValue();

            Citation response =
                    Citation.builder()
                            .title(doc.getString("title"))
                            .description(doc.getString("description"))
                            .content(doc.getString("content"))
                            .url(doc.getString("url"))
                            .score(
                                    doc.get("score") != null
                                            ? ((Number) doc.get("score")).doubleValue()
                                            : 0.0
                            )
                            .fake_probability(fakeProbability)
                            .real_probability(realProbability)
                            .build();

            responses.add(response);
        }

        return responses;
    }
}