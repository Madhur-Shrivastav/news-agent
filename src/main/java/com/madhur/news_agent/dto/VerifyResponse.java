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
    private int fake_probability;
    private int true_probability;
    private String reasoning;        // short reasoning
    private List<String> citations;  // urls of evidence
}
