package com.madhur.news_agent.models;

import com.madhur.news_agent.dto.ChatMessage;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    private String role;
    @Field("payload")
    private ChatMessage payload;
    private String message;
    private Instant ts;
}
