package com.madhur.news_agent.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiLLMService {

    private final Client client;

    public GeminiLLMService(@Value("${gemini.api.key}") String apiKey) {
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();
    }

    public String generateText(String prompt) {
        GenerateContentResponse response = client.models.generateContent(
                "gemini-2.0-flash",
                prompt,
                null
        );
        return response.text();
    }

    public String generateJson(String prompt) {
        GenerateContentResponse response = client.models.generateContent(
                "gemini-2.0-flash",
                prompt,
                null
        );
        return response.text();
    }
}
