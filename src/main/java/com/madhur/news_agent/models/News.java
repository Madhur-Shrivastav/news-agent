package com.madhur.news_agent.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "news_articles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class News {

    @Id
    private String id;

    private String sourceName;
    private String author;

    private String title;
    private String description;
    private String content;

    private String url;
    private String urlToImage;

    private Instant publishedAt;

    private String hash;
    private List<Float> embedding;
}
