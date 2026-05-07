package com.madhur.news_agent.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyResponse {
    private String verdict;
    private String reasoning;
    private List<Citation> citations;
}
