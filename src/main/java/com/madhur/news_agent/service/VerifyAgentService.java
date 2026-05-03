package com.madhur.news_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madhur.news_agent.dto.ChatMessage;
import com.madhur.news_agent.dto.VerifyResponse;
import com.madhur.news_agent.models.Message;
import org.bson.Document;
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

        List<Document> evidence =
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


        for (Document document : evidence) System.out.println("Evidence: " + document);

        if (evidence.isEmpty()) {
            return ChatMessage.builder()
                    .sessionId("")
                    .intent("VERIFY")
                    .reply("I couldn't find any relevant evidence articles in the database for this claim.")
                    .verification(
                            VerifyResponse.builder()
                                    .verdict("UNVERIFIED")
                                    .fake_probability(10)
                                    .true_probability(90)
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

    private String buildPrompt(String claim, List<Document> evidence, String intent, List<Message> messages) {

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
- confidence must be an integer 0-100
- reasoning must be short (max 4 lines)
- citations MUST be URLs from the evidence only (no invented citations)

Claim:
""").append(claim).append("\n\n");

        sb.append("User intent:\n").append(intent).append("\n\n");

        sb.append("Evidence Articles:\n\n");

        for (int i = 0; i < evidence.size(); i++) {
            Document a = evidence.get(i);
            sb.append("[").append(i + 1).append("]\n");
            sb.append("Title: ").append(nullSafe(a.getString("title"))).append("\n");
            sb.append("Description: ").append(nullSafe(a.getString("description"))).append("\n");
            sb.append("Content: ").append(truncate(nullSafe(a.getString("content")), 1200)).append("\n");
            sb.append("URL: ").append(nullSafe(a.getString("url"))).append("\n");
            sb.append("Similarity Score: ").append(a.get("score")).append("\n\n");
            sb.append("Fake Probability: ")
                    .append(a.get("fake_probability") != null ? a.get("fake_probability") : "N/A")
                    .append("\n");
            sb.append("True Probability: ")
                    .append(a.get("real_probability") != null ? a.get("real_probability") : "N/A")
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
    "fake_probability": int,
    "true_probability": int,
    "reasoning": "string",
    "citations": ["url1", "url2", ... "url5"]
  }
}
""");

        return sb.toString();
    }


    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }

    private ChatMessage safeParse(String raw, List<Document> evidence) {
        try {
            String json = extractJson(raw);

            ChatMessage parsed = mapper.readValue(json, ChatMessage.class);

            if (parsed.getVerification() == null) {
                parsed.setVerification(new VerifyResponse());
            }

            VerifyResponse vr = parsed.getVerification();

            List<String> allowed = evidence.stream()
                    .map(d -> d.getString("url"))
                    .toList();

            List<String> filtered = new ArrayList<>();
            if (vr.getCitations() != null) {
                for (String c : vr.getCitations()) {
                    if (allowed.contains(c)) filtered.add(c);
                }
            }
            vr.setCitations(filtered);

            if (vr.getVerdict() == null) vr.setVerdict("UNVERIFIED");
            vr.setVerdict(vr.getVerdict().toUpperCase());

            if (!vr.getVerdict().equals("TRUE") &&
                    !vr.getVerdict().equals("FALSE") &&
                    !vr.getVerdict().equals("UNVERIFIED")) {
                vr.setVerdict("UNVERIFIED");
            }

            if (vr.getReasoning() == null) vr.setReasoning("");

            parsed.setVerification(vr);

            if (parsed.getReply() == null) parsed.setReply("");
            if (parsed.getIntent() == null) parsed.setIntent("VERIFY");
            if (parsed.getSessionId() == null) parsed.setSessionId("");

            return parsed;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ChatMessage.builder()
                    .sessionId("")
                    .intent("VERIFY")
                    .reply("I found some evidence articles, but could not parse the model response cleanly.")
                    .verification(
                            VerifyResponse.builder()
                                    .verdict("UNVERIFIED")
                                    .fake_probability(40)
                                    .true_probability(60)
                                    .reasoning("Could not parse model response. Evidence retrieved but output format was invalid.")
                                    .citations(evidence.stream().map(d -> d.getString("url")).limit(3).toList())
                                    .build()
                    )
                    .build();
        }
    }

    private String extractJson(String raw) {
        raw = raw.trim();

        int start = raw.indexOf('{');
        if (start < 0) return raw;

        int depth = 0;
        for (int i = start; i < raw.length(); i++) {
            char ch = raw.charAt(i);

            if (ch == '{') depth++;
            else if (ch == '}') depth--;

            if (depth == 0) {
                return raw.substring(start, i + 1);
            }
        }

        return raw.substring(start);
    }
}

//package com.madhur.news_agent.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.madhur.news_agent.dto.ChatMessage;
//import com.madhur.news_agent.dto.VerifyResponse;
//import org.bson.Document;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class VerifyAgentService {
//
//    private final OllamaEmbeddingService embeddingService;
//    private final VectorSearchService vectorSearchService;
//    private final OllamaLLMService llmService;
//
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    public VerifyAgentService(OllamaEmbeddingService embeddingService,
//                              VectorSearchService vectorSearchService,
//                              OllamaLLMService llmService) {
//        this.embeddingService = embeddingService;
//        this.vectorSearchService = vectorSearchService;
//        this.llmService = llmService;
//    }
//
//    // ✅ Return ChatResponse now
//    public ChatMessage verify(String claim, String intent) {
//
//        List<Float> queryVec = embeddingService.embed(claim);
//        List<Document> evidence = vectorSearchService.searchRaw(queryVec, 6);
//
//        for (Document document : evidence) System.out.println("Evidence: " + document);
//
//        if (evidence.isEmpty()) {
//            return ChatMessage.builder()
//                    .sessionId("")
//                    .intent("VERIFY")
//                    .reply("I couldn't find any relevant evidence articles in the database for this claim.")
//                    .verification(
//                            VerifyResponse.builder()
//                                    .verdict("UNVERIFIED")
//                                    .confidence(10)
//                                    .reasoning("No relevant evidence articles found in the database.")
//                                    .citations(List.of())
//                                    .build()
//                    )
//                    .build();
//        }
//
//        String prompt = buildPrompt(claim, evidence, intent);
//        String raw = llmService.generateJson(prompt).trim();
//
//        return safeParse(raw, evidence);
//    }
//
//    private String buildPrompt(String claim, List<Document> evidence, String intent) {
//
//        StringBuilder sb = new StringBuilder();
//
//        sb.append("""
//You are a News Verification Agent.
//You must respond ONLY using the evidence articles below.
//Do NOT invent facts.
//Do NOT invent citations.
//
//Return STRICT JSON ONLY (no markdown, no extra text).
//
//Rules:
//- intent must be exactly: CHAT or VERIFY
//- sessionId must be a short string (you may generate one)
//- reply: should be a natural helpful reply to the user (max 4-5 lines)
//- verdict must be exactly one of: TRUE, FALSE, UNVERIFIED
//- confidence must be an integer 0-100
//- reasoning must be short (max 4 lines)
//- citations MUST be URLs from the evidence only (no invented citations)
//
//Claim:
//""").append(claim).append("\n\n");
//
//        sb.append("User intent:\n").append(intent).append("\n\n");
//
//        sb.append("Evidence Articles:\n\n");
//
//        for (int i = 0; i < evidence.size(); i++) {
//            Document a = evidence.get(i);
//            sb.append("[").append(i + 1).append("]\n");
//            sb.append("Title: ").append(nullSafe(a.getString("title"))).append("\n");
//            sb.append("Description: ").append(nullSafe(a.getString("description"))).append("\n");
//            sb.append("Content: ").append(truncate(nullSafe(a.getString("content")), 1200)).append("\n");
//            sb.append("URL: ").append(nullSafe(a.getString("url"))).append("\n");
//            sb.append("Score: ").append(a.get("score")).append("\n\n");
//        }
//
//        sb.append("""
//Return JSON in EXACTLY this format:
//{
//  "sessionId": "string",
//  "intent": "CHAT or VERIFY",
//  "reply": "string",
//  "verification": {
//    "verdict": "TRUE|FALSE|UNVERIFIED",
//    "confidence": 0,
//    "reasoning": "string",
//    "citations": ["url1", "url2"]
//  }
//}
//""");
//
//        return sb.toString();
//    }
//
//    private String nullSafe(String s) {
//        return s == null ? "" : s;
//    }
//
//    private String truncate(String s, int max) {
//        if (s == null) return "";
//        if (s.length() <= max) return s;
//        return s.substring(0, max) + "...";
//    }
//
//    private ChatMessage safeParse(String raw, List<Document> evidence) {
//
//        try {
//            String json = extractJson(raw);
//
//            ChatMessage parsed = mapper.readValue(json, ChatMessage.class);
//
//            // Ensure verification exists
//            if (parsed.getVerification() == null) {
//                parsed.setVerification(new VerifyResponse());
//            }
//
//            VerifyResponse vr = parsed.getVerification();
//
//            // Allowed citations
//            List<String> allowed = evidence.stream()
//                    .map(d -> d.getString("url"))
//                    .toList();
//
//            // Filter citations
//            List<String> filtered = new ArrayList<>();
//            if (vr.getCitations() != null) {
//                for (String c : vr.getCitations()) {
//                    if (allowed.contains(c)) filtered.add(c);
//                }
//            }
//            vr.setCitations(filtered);
//
//            // normalize verdict
//            if (vr.getVerdict() == null) vr.setVerdict("UNVERIFIED");
//            vr.setVerdict(vr.getVerdict().toUpperCase());
//
//            if (!vr.getVerdict().equals("TRUE") &&
//                    !vr.getVerdict().equals("FALSE") &&
//                    !vr.getVerdict().equals("UNVERIFIED")) {
//                vr.setVerdict("UNVERIFIED");
//            }
//
//            // clamp confidence
//            if (vr.getConfidence() < 0) vr.setConfidence(0);
//            if (vr.getConfidence() > 100) vr.setConfidence(100);
//
//            if (vr.getReasoning() == null) vr.setReasoning("");
//
//            parsed.setVerification(vr);
//
//            // fallback reply
//            if (parsed.getReply() == null) parsed.setReply("");
//            if (parsed.getIntent() == null) parsed.setIntent("VERIFY");
//            if (parsed.getSessionId() == null) parsed.setSessionId("");
//
//            return parsed;
//
//        } catch (Exception e) {
//
//            return ChatMessage.builder()
//                    .sessionId("")
//                    .intent("VERIFY")
//                    .reply("I found some evidence articles, but could not parse the model response cleanly.")
//                    .verification(
//                            VerifyResponse.builder()
//                                    .verdict("UNVERIFIED")
//                                    .confidence(40)
//                                    .reasoning("Could not parse model response. Evidence retrieved but output format was invalid.")
//                                    .citations(evidence.stream().map(d -> d.getString("url")).limit(3).toList())
//                                    .build()
//                    )
//                    .build();
//        }
//    }
//
//    // ✅ Stronger JSON extraction
//    private String extractJson(String raw) {
//        raw = raw.trim();
//
//        int start = raw.indexOf('{');
//        if (start < 0) return raw;
//
//        int depth = 0;
//        for (int i = start; i < raw.length(); i++) {
//            char ch = raw.charAt(i);
//
//            if (ch == '{') depth++;
//            else if (ch == '}') depth--;
//
//            if (depth == 0) {
//                return raw.substring(start, i + 1);
//            }
//        }
//
//        return raw.substring(start);
//    }
//}
