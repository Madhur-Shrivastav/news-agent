package com.madhur.news_agent.dto;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Citation {
    private String title;
    private String description;
    private String content;
    private String url;
    private double score;
    private double fake_probability;
    private double real_probability;
}