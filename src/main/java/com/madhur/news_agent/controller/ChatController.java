package com.madhur.news_agent.controller;
import com.madhur.news_agent.dto.ChatMessage;
import com.madhur.news_agent.service.ChatAgentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatAgentService chatAgentService;

    public ChatController(ChatAgentService chatAgentService) {
        this.chatAgentService = chatAgentService;
    }

    @GetMapping("/chat")
    public ChatMessage chat(@RequestParam String userId,
                            @RequestParam String sessionId,
                            @RequestParam String message) throws Exception {
        return chatAgentService.chat(userId, sessionId ,message);
    }
}
