package com.madhur.news_agent.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    private String sessionId;
    private String intent;
    private String reply;
    private VerifyResponse verification;
}
