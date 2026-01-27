package com.madhur.news_agent.repository;

import com.madhur.news_agent.models.Session;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends MongoRepository<Session, String> {
    Optional<Session> findByUserId(String userId);
    Optional<Session> findByUserIdAndSessionId(String userId, String sessionId);
    List<Session> findAllByUserId(String userId);

}
