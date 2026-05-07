package com.madhur.news_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madhur.news_agent.dto.ChatMessage;
import com.madhur.news_agent.dto.Citation;
import com.madhur.news_agent.dto.VerifyResponse;
import com.madhur.news_agent.models.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class VerifyAgentService {

    private final EmbeddingsService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final GeminiLLMService llmService;
    private final ObjectMapper mapper = new ObjectMapper();

    public VerifyAgentService(EmbeddingsService embeddingService,
                              VectorSearchService vectorSearchService,
                              GeminiLLMService llmService) {
        this.embeddingService = embeddingService;
        this.vectorSearchService = vectorSearchService;
        this.llmService = llmService;
    }

    public ChatMessage verify(String claim, String intent, List<String> entities, List<String> keywords, List<Message> messages) {

        List<Float> queryVec =
                embeddingService.embed(claim);

        List<Citation> evidence =
                vectorSearchService.searchRaw(
                        queryVec,
                        10,
                        entities.stream()
                                .map(e -> e.toLowerCase().trim())
                                .distinct()
                                .toList(),
                        keywords.stream()
                                .map(e -> e.toLowerCase().trim())
                                .distinct()
                                .toList()
                );


        for (Citation probability : evidence) System.out.println("Probability: " + probability);

        if (evidence.isEmpty()) {

            return ChatMessage.builder()
                    .sessionId("")
                    .intent("VERIFY")
                    .reply("I couldn't find any relevant evidence articles in the database for this claim.")
                    .verification(
                            VerifyResponse.builder()
                                    .verdict("UNVERIFIED")
                                    .reasoning("No relevant evidence articles found in the database.")
                                    .citations(List.of())
                                    .build()
                    )
                    .build();
        }

        String prompt = buildPrompt(claim, evidence, intent, messages);
        String raw = llmService.generateJson(prompt).trim();

        return safeParse(raw, evidence);
    }

    private String buildPrompt(String claim, List<Citation> evidence, String intent, List<Message> messages) {

        StringBuilder sb = new StringBuilder();

        sb.append("""
You are a News Verification Agent.
You must respond ONLY using the evidence articles below.
Do NOT invent facts.
Do NOT invent citations.

Return STRICT JSON ONLY (no markdown, no extra text).

Rules:
- intent must be exactly: CHAT or VERIFY
- sessionId must be a short string (you may generate one)
- reply: should be a natural helpful reply to the user (max 4-5 lines)
- verdict must be exactly one of: TRUE, FALSE, UNVERIFIED
- reasoning must be short (max 4 lines)
- citations MUST contain full citation objects copied from the evidence below
- NEVER invent citation objects
- NEVER invent URLs

Claim:
""").append(claim).append("\n\n");

        sb.append("User intent:\n").append(intent).append("\n\n");

        sb.append("Evidence Articles:\n\n");

        for (int i = 0; i < evidence.size(); i++) {

            Citation a = evidence.get(i);

            sb.append("[").append(i + 1).append("]\n");

            sb.append("Title: ")
                    .append(nullSafe(a.getTitle()))
                    .append("\n");

            sb.append("Description: ")
                    .append(nullSafe(a.getDescription()))
                    .append("\n");

            sb.append("Content: ")
                    .append(truncate(
                            nullSafe(a.getContent())
                                    .replace("\"", "'")
                                    .replace("\n", " ")
                                    .replace("\r", " ")
                    ))
                    .append("\n");

            sb.append("URL: ")
                    .append(nullSafe(a.getUrl()))
                    .append("\n");

            sb.append("Similarity Score: ")
                    .append(a.getScore())
                    .append("\n\n");

            sb.append("Fake Probability: ")
                    .append(a.getFake_probability())
                    .append("\n");

            sb.append("Real Probability: ")
                    .append(a.getReal_probability())
                    .append("\n\n");
        }

        sb.append("\nUSER CHAT HISTORY (for context only):\n");

        if (messages == null || messages.isEmpty()) {
            sb.append("No prior messages.\n");
        } else {
            int max = 50;
            int start = Math.max(0, messages.size() - max);

            for (int i = start; i < messages.size(); i++) {
                sb.append("- ").append(messages.get(i).getMessage()).append("\n");
            }
        }

        sb.append("""
        Return JSON in EXACTLY this format:
        {
          "sessionId": "string",
          "intent": "CHAT or VERIFY",
          "reply": "string",
          "verification": {
            "verdict": "TRUE|FALSE|UNVERIFIED",
            "reasoning": "string",
            "citations": [
              {
                "title": "string",
                "description": "string",
                "content": "string",
                "url": "string",
                "score": 0.0,
                "fake_probability": 0.0,
                "real_probability": 0.0
              }
            ]
          }
        }
""");

        return sb.toString();
    }


    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s) {
        if (s == null) return "";
        if (s.length() <= 1200) return s;
        return s.substring(0, 1200) + "...";
    }

    private ChatMessage safeParse(String raw, List<Citation> evidence) {

        try {

            String json = extractJson(raw);

            ChatMessage parsed =
                    mapper.readValue(json, ChatMessage.class);

            if (parsed.getVerification() == null) {
                parsed.setVerification(new VerifyResponse());
            }

            VerifyResponse vr = parsed.getVerification();

            List<String> allowed = evidence.stream()
                    .map(Citation::getUrl)
                    .toList();


            List<Citation> filtered = new ArrayList<>();

            if (vr.getCitations() != null) {

                for (Citation c : vr.getCitations()) {

                    if (c != null &&
                            c.getUrl() != null &&
                            allowed.contains(c.getUrl())) {

                        filtered.add(c);
                    }
                }
            }

            vr.setCitations(filtered);

            if (vr.getVerdict() == null) {
                vr.setVerdict("UNVERIFIED");
            }

            vr.setVerdict(vr.getVerdict().toUpperCase());

            if (!vr.getVerdict().equals("TRUE") &&
                    !vr.getVerdict().equals("FALSE") &&
                    !vr.getVerdict().equals("UNVERIFIED")) {

                vr.setVerdict("UNVERIFIED");
            }

            if (vr.getReasoning() == null) {
                vr.setReasoning("");
            }

            parsed.setVerification(vr);


            if (parsed.getReply() == null) {
                parsed.setReply("");
            }

            if (parsed.getIntent() == null) {
                parsed.setIntent("VERIFY");
            }

            if (parsed.getSessionId() == null) {
                parsed.setSessionId("");
            }

            return parsed;

        } catch (Exception e) {

            e.printStackTrace();

            System.out.println("========== RAW ==========");
            System.out.println(raw);

            System.out.println("========== JSON ==========");
            System.out.println(extractJson(raw));

            return ChatMessage.builder()
                    .sessionId("")
                    .intent("VERIFY")
                    .reply("I found some evidence articles, but could not parse the model response cleanly.")
                    .verification(
                            VerifyResponse.builder()
                                    .verdict("UNVERIFIED")
                                    .reasoning("Could not parse model response. Evidence retrieved but output format was invalid.")
                                    .citations(
                                            evidence.stream()
                                                    .limit(3)
                                                    .toList()
                                    )
                                    .build()
                    )
                    .build();
        }
    }

    private String extractJson(String raw) {

        raw = raw.trim();

        int start = raw.indexOf('{');

        int end = raw.lastIndexOf('}');

        if (start < 0 || end < 0 || end <= start) {
            return raw;
        }

        return raw.substring(start, end + 1);
    }
}