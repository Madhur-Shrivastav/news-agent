package com.madhur.news_agent.service;

import com.madhur.news_agent.models.News;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSearchService {

    private final MongoTemplate mongoTemplate;

    public VectorSearchService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Document> searchRaw(List<Float> queryVector, int limit) {

        Document vectorStage = new Document("$vectorSearch",
                new Document("index", "vector_index")
                        .append("path", "embedding")
                        .append("queryVector", queryVector)
                        .append("numCandidates", 200)
                        .append("limit", limit)
        );

        Document projectStage = new Document("$project",
                new Document("title", 1)
                        .append("description", 1)
                        .append("url", 1)
                        .append("publishedAt", 1)
                        .append("sourceName", 1)
                        .append("score", new Document("$meta", "vectorSearchScore"))
        );

        Aggregation agg = Aggregation.newAggregation(
                context -> vectorStage,
                context -> projectStage
        );

        return mongoTemplate.aggregate(agg, "news_articles", Document.class)
                .getMappedResults();
    }
}
