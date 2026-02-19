package com.madhur.news_agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntentResponse {

    private String intent;          // CHAT or VERIFY
    private String optimizedQuery;  // rewritten claim
    private String collection;      // target collection
    private List<String> entities;
    private List<String> keywords;
}
