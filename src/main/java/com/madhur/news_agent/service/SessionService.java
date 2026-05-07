package com.madhur.news_agent.service;

import com.madhur.news_agent.dto.SessionSummary;
import com.madhur.news_agent.models.Message;
import com.madhur.news_agent.models.Session;
import com.madhur.news_agent.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class SessionService {

    private final SessionRepository repo;

    public SessionService(SessionRepository repo) {
        this.repo = repo;
    }

    public List<SessionSummary> getSessionsByUser(String userId) {
        return repo.findAllByUserId(userId)
                .stream()
                .map(s -> new SessionSummary(
                        s.getSessionId(),
                        s.getCreated_at(),
                        s.getUpdated_at()
                ))
                .toList();
    }

    public List<Message> getSessionMessages(String userId, String sessionId) {

        Session session = repo.findByUserIdAndSessionId(userId, sessionId)
                .orElseGet(() -> {
                    Session newSession = Session.builder()
                            .userId(userId)
                            .sessionId(sessionId)
                            .created_at(Instant.now())
                            .updated_at(Instant.now())
                            .messages(new ArrayList<>())
                            .summary("")
                            .build();

                    return repo.save(newSession);
                });

        return session.getMessages();
    }

    public void deleteSession(String userId, String sessionId) {

        Session session = repo.findByUserIdAndSessionId(userId, sessionId)
                .orElseThrow(() ->
                        new RuntimeException("Session not found")
                );

        repo.delete(session);
    }
}