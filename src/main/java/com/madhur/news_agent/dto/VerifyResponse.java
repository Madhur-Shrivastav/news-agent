package com.madhur.news_agent.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyResponse {
    private String verdict;          // TRUE / FALSE / UNVERIFIED
    private int confidence;          // 0-100
    private String reasoning;        // short reasoning
    private List<String> citations;  // urls of evidence
}
