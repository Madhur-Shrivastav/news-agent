package com.madhur.news_agent.service;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;


@Service
public class VectorSearchService {

    private final MongoTemplate mongoTemplate;

    public VectorSearchService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Document> searchRaw(
            List<Float> queryVector,
            int limit,
            List<String> entities,
            List<String> keywords
    ) {

        List<String> normalizedEntities = entities == null ? List.of() :
                entities.stream()
                        .map(e -> e.toLowerCase().trim())
                        .distinct()
                        .toList();

        List<String> normalizedKeywords = keywords == null ? List.of() :
                keywords.stream()
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
                    new Document("entities",
                            new Document("$in", normalizedEntities))
            );
        }

        if (!normalizedKeywords.isEmpty()) {
            conditions.add(
                    new Document("keywords",
                            new Document("$in", normalizedKeywords))
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

        Document vectorStage = new Document("$vectorSearch", vectorStageBody);

        Document projectStage = new Document("$project",
                new Document("title", 1)
                        .append("description", 1)
                        .append("url", 1)
                        .append("publishedAt", 1)
                        .append("sourceName", 1)
                        .append("entities", 1)
                        .append("keywords", 1)
                        .append("score", new Document("$meta", "vectorSearchScore"))
        );

        Aggregation agg = Aggregation.newAggregation(
                context -> vectorStage,
                context -> projectStage
        );

        List<Document> results = mongoTemplate
                .aggregate(agg, "news_articles", Document.class)
                .getMappedResults();

        if (results.isEmpty()) {

            Document fallbackStage = new Document("$vectorSearch",
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
                    .aggregate(fallbackAgg, "news_articles", Document.class)
                    .getMappedResults();
        }

        return results;
    }

}
