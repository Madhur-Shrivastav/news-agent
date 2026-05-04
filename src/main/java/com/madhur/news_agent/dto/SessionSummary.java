package com.madhur.news_agent.dto;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SessionSummary {
    private String sessionId;
    private Instant created_at;
    private Instant updated_at;
}
