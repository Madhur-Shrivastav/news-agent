package com.madhur.news_agent.repository;

import com.madhur.news_agent.models.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NewsRepository extends MongoRepository<News, String> {
    Optional<News> findByHash(String hash);
    List<News> findByEmbeddingIsNull();
    Page<News> findAll(Pageable pageable);
}


