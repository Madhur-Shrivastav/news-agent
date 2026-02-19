package com.madhur.news_agent.controller;

import com.madhur.news_agent.service.NewsIngestionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class IngestionController {

    private final NewsIngestionService ingestionService;

    public IngestionController(NewsIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @GetMapping("/ingest")
    public String ingest(@RequestParam String q,
                         @RequestParam(required = false, defaultValue = "2026-01-19") String from,
                         @RequestParam(required = false, defaultValue = "1") String page)
            throws Exception {

        int inserted = ingestionService.ingest(q, from, page);
        return "✅ Inserted: " + inserted + " for " + q;
    }
}
