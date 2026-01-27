package com.madhur.news_agent.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "user_chats")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    private String sessionId;

    private Instant created_at;
    private Instant updated_at;

    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    private String summary;
}

