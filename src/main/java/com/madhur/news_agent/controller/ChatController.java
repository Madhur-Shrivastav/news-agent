package com.madhur.news_agent.controller;
import com.madhur.news_agent.dto.ChatMessage;
import com.madhur.news_agent.dto.ChatRequest;
import com.madhur.news_agent.dto.SessionSummary;
import com.madhur.news_agent.models.Message;
import com.madhur.news_agent.service.ChatAgentService;
import com.madhur.news_agent.service.SessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    private String userId;
    private String message;

    private final ChatAgentService chatAgentService;
    private final SessionService sessionService;
    public ChatController(ChatAgentService chatAgentService, SessionService sessionService) {
        this.chatAgentService = chatAgentService;
        this.sessionService = sessionService;
    }

    @PostMapping("/chat/{sessionId}")
    public ChatMessage chat(
            @PathVariable String sessionId,
            @RequestBody ChatRequest request
    ) throws Exception {

        return chatAgentService.chat(
                request.getUserId(),
                sessionId,
                request.getMessage()
        );
    }

    @GetMapping("/sessions/{userId}")
    public List<SessionSummary> getUserSessions(@PathVariable String userId) {
        return sessionService.getSessionsByUser(userId);
    }

    @GetMapping("/sessions/{userId}/{sessionId}")
    public List<Message> getSessionMessages(@PathVariable String userId, @PathVariable String sessionId) {
        return sessionService.getSessionMessages(userId,sessionId);
    }
}
