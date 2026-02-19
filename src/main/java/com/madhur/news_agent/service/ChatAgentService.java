package com.madhur.news_agent.service;
import com.madhur.news_agent.dto.IntentResponse;
import com.madhur.news_agent.intents.IntentType;
import com.madhur.news_agent.dto.ChatMessage;
import com.madhur.news_agent.models.Message;
import com.madhur.news_agent.models.Session;
import com.madhur.news_agent.repository.SessionRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatAgentService {

    private final SessionRepository repo;
    private final VerifyAgentService verifyAgentService;
    private final NormalChatService normalChatService;
    private final IntentAgentService intentAgentService;

    public ChatAgentService(SessionRepository repo,
                            VerifyAgentService verifyAgentService,
                            NormalChatService normalChatService, IntentAgentService intentAgentService) {
        this.repo = repo;
        this.verifyAgentService = verifyAgentService;
        this.normalChatService = normalChatService;
        this.intentAgentService = intentAgentService;
    }

    public ChatMessage chat(String userId, String sessionId, String userMessage) throws Exception {

        Session session = repo.findByUserIdAndSessionId(userId, sessionId).orElseGet(() ->
                Session.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .created_at(Instant.now())
                        .updated_at(Instant.now())
                        .summary(null)
                        .messages(new ArrayList<>())
                        .build()
        );

        if (session.getMessages() == null)
            session.setMessages(new ArrayList<>());

        session.getMessages().add(Message.builder()
                .role("user")
                .payload(null)
                .message(userMessage)
                .ts(Instant.now())
                .build());

        IntentResponse intentResponse = intentAgentService.detectIntent(userMessage);

        IntentType intent =
                intentResponse.getIntent().equalsIgnoreCase("VERIFY")
                        ? IntentType.VERIFY
                        : IntentType.CHAT;

        String optimizedQuery = intentResponse.getOptimizedQuery();
        List<String> entities = intentResponse.getEntities();
        List<String> keywords = intentResponse.getKeywords();
        //String collection = intentResponse.getCollection();

        System.out.println("User Intent: " + intent);
        System.out.println("Optimized Query: " + optimizedQuery);
        System.out.println("Entities Extracted: " + entities);
        System.out.println("Keywords Extracted: " + keywords);
        //System.out.println("Collection: " + collection);

        ChatMessage response;

        if (intent == IntentType.VERIFY) {
            response = verifyAgentService.verify(
                    optimizedQuery,
                    intent.toString(),
                    entities,
                    keywords,
                    session.getMessages()
            );
        } else {
            response = normalChatService.chat(
                    optimizedQuery,
                    //collection,
                    intent.toString(),
                    session.getMessages()
            );
        }

        System.out.println("Assistant Response: " + response);

        response.setSessionId(sessionId);
        response.setIntent(intent.toString());

        session.getMessages().add(Message.builder()
                .role("assistant")
                .payload(response)
                .message(response.getReply())
                .ts(Instant.now())
                .build());

        session.setUpdated_at(Instant.now());
        repo.save(session);

        return response;
    }

}
