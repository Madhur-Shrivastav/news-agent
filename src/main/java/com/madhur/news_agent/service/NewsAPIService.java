package com.madhur.news_agent.service;

import com.madhur.news_agent.dto.NewsApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NewsAPIService {

    private final RestClient restClient;

    @Value("${newsapi.api-key}")
    private String apiKey;

    public NewsAPIService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://newsapi.org")
                .build();
    }

    public NewsApiResponse fetchNews(String query, String fromDate, String page) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/everything")
                        .queryParam("q", query)
                        .queryParam("from", fromDate)
                        .queryParam("sortBy", "publishedAt")
                        .queryParam("apiKey", apiKey)
                        .queryParam("language", "en")
                        //.queryParam("country","us")
                        .queryParam("pageSize", 100)
                        .queryParam("page", page)
                        .queryParam("searchIn", "title,description,content")
                        .build())
                .retrieve()
                .body(NewsApiResponse.class);
    }
}
