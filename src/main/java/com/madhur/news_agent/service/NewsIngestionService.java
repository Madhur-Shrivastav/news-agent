package com.madhur.news_agent.service;

import com.madhur.news_agent.dto.NewsApiResponse;
import com.madhur.news_agent.models.News;
import com.madhur.news_agent.repository.NewsRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class NewsIngestionService {

    private final NewsAPIService newsApiClient;
    private final EmbeddingsService embeddingsService;
    private  final MongoTemplate mongoTemplate;
    private final NewsRepository repo;
    private final HashSet<String> articles = new HashSet<>();

    public NewsIngestionService(NewsAPIService newsApiClient, EmbeddingsService embeddingsService, NewsRepository repo, MongoTemplate mongoTemplate) {
        this.newsApiClient = newsApiClient;
        this.embeddingsService = embeddingsService;
        this.mongoTemplate = mongoTemplate;
        this.repo = repo;
    }

//    public int ingest(String query, String fromDate, String page) throws Exception {
//        NewsApiResponse response = newsApiClient.fetchNews(query, fromDate, page);
//
//        if (response == null || response.getArticles() == null) return 0;
//
//        System.out.println("Total articles fetched for " + query + " are " + response.getTotalResults());
//
//        int inserted = 0;
//
//        for (var a : response.getArticles()) {
//
//            if (a.getTitle() == null || a.getUrl() == null) continue;
//
//            String hash = sha256((a.getTitle() + "|" + a.getUrl()).toLowerCase());
//            articles.add(hash);
//            System.out.println(hash);
//            if (repo.findByHash(hash).isPresent()) continue;
//
//            String title = a.getTitle() == null ? "" : a.getTitle();
//            String desc = a.getDescription() == null ? "" : a.getDescription();
//            String content = a.getContent() == null ? "" : a.getContent();
//            String text = ("Title: " + title + "\nDescription: " + desc + "\nContent: " + content).trim();
//            List<Float> embedding = new ArrayList<>();
//
//            if (!text.isBlank()) {
//                try {
//                    embedding = embeddingsService.embed(text);
//                } catch (Exception e) {
//                    System.out.println("❌ Embedding failed for: " + a.getTitle());
//                    System.out.println("Reason: " + e.getMessage());
//                }
//            }
//
//            News doc = News.builder()
//                    .sourceName(a.getSource() != null ? a.getSource().getName() : null)
//                    .author(a.getAuthor())
//                    .title(a.getTitle())
//                    .description(a.getDescription())
//                    .content(a.getContent())
//                    .url(a.getUrl())
//                    .urlToImage(a.getUrlToImage())
//                    .publishedAt(a.getPublishedAt() != null ? Instant.parse(a.getPublishedAt()) : null)
//                    .hash(hash)
//                    .embedding(embedding)
//                    .build();
//
//            mongoTemplate.insert(doc, query.toLowerCase());
//            inserted++;
//        }
//        System.out.println("Unique hashes: " + articles.size());
//        return inserted;
//    }

    private String fetchFullContent(String url) {
        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            // 🔥 Try multiple selectors (fallback strategy)
            String content = doc.select("article p").text();

            if (content.isBlank()) {
                content = doc.select("div[itemprop=articleBody] p").text();
            }

            if (content.isBlank()) {
                content = doc.select("p").text();
            }

            // 🔥 Basic cleaning (safe, not destructive)
            content = content
                    .replaceAll("\\s+", " ")
                    .trim();

            // 🔥 Hard cutoff for common junk sections
            int idx = content.toLowerCase().indexOf("disclaimer");
            if (idx > 0) {
                content = content.substring(0, idx);
            }

            // 🔥 Quality filter
            if (content.length() < 200) {
                return ""; // too weak → skip
            }

            return content;

        } catch (Exception e) {
            return "";
        }
    }

    public int ingest(String query, String fromDate, String page) throws Exception {

        NewsApiResponse response = newsApiClient.fetchNews(query, fromDate, page);

        if (response == null || response.getArticles() == null) return 0;

        System.out.println("Fetched for " + query + ": " + response.getTotalResults());

        int inserted = 0;

        for (var a : response.getArticles()) {

            if (a.getTitle() == null || a.getUrl() == null) continue;

            String hash = sha256((a.getUrl()).toLowerCase());
            System.out.println("Hash:" + hash);
            if (repo.findByHash(hash).isPresent()) continue;
            String title = a.getTitle() == null ? "" : a.getTitle();
            String desc = a.getDescription() == null ? "" : a.getDescription();

            String content = fetchFullContent(a.getUrl());
            System.out.println("Full Content: " + content);

            int MAX_CHARS = 7000;

            if (content.length() == 0) continue;

            if (content.length() > MAX_CHARS) {
                content = content.substring(0, MAX_CHARS);
            }

            String text = ("Title: " + title +
                    "\nDescription: " + desc +
                    "\nContent: " + content).trim();

            List<Float> embedding = new ArrayList<>();

            if (!text.isBlank() && text.length() > 50) {
                try {
                    embedding = embeddingsService.embed(text);
                } catch (Exception e) {
                    System.out.println("Embedding failed: " + title);
                    System.out.println("Reason" + e.getMessage());
                }
            }

            Instant published = null;
            try {
                if (a.getPublishedAt() != null) {
                    published = Instant.parse(a.getPublishedAt());
                }
            } catch (Exception ignored) {}

            News doc = News.builder()
                    .sourceName(a.getSource() != null ? a.getSource().getName() : null)
                    .author(a.getAuthor())
                    .title(title)
                    .description(desc)
                    .content(content)
                    .url(a.getUrl())
                    .urlToImage(a.getUrlToImage())
                    .publishedAt(published)
                    .hash(hash)
                    .embedding(embedding)
                    .build();

            try {
                mongoTemplate.insert(doc, "news_articles");
                inserted++;
            } catch (Exception e) {
                System.out.println("❌ Embedding failed for: " + a.getTitle());
                System.out.println("Reason: " + e.getMessage());
            }
        }

        return inserted;
    }


    private static String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
