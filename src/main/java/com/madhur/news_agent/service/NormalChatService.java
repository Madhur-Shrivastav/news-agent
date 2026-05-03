package com.madhur.news_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madhur.news_agent.dto.ChatMessage;
import com.madhur.news_agent.dto.VerifyResponse;
import com.madhur.news_agent.models.Message;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NormalChatService {


    private final GeminiLLMService llmService;

    private final ObjectMapper mapper = new ObjectMapper();

    public NormalChatService(GeminiLLMService llmService) {
        this.llmService = llmService;
    }

    public ChatMessage chat(String userRequest, String intent, List<Message> messages) {

        String prompt = buildPrompt(userRequest, intent, messages);

        String raw = llmService.generateJson(prompt).trim();

        return safeParse(raw);
    }

    private String buildPrompt(String userRequest, String intent, List<Message> messages) {

        StringBuilder sb = new StringBuilder();

        sb.append("""
You are a News Agent for chatting with the user normally.

Return STRICT JSON ONLY (no markdown, no extra text).

Rules:
- intent must be exactly: CHAT or VERIFY
- sessionId must be a short string (you may generate one)
- reply: should be a natural helpful reply to the user (max 4-5 lines)

User Request:
""").append(userRequest).append("\n\n");

        sb.append("User intent:\n").append(intent).append("\n\n");

        sb.append("\nUSER CHAT HISTORY (for context only):\n");

        if (messages == null || messages.isEmpty()) {
            sb.append("No prior messages.\n");
        } else {
            int max = 10;
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
  "verification": null
}
""");

        return sb.toString();
    }

    private ChatMessage safeParse(String raw) {

        try {
            String json = extractJson(raw);

            ChatMessage parsed = mapper.readValue(json, ChatMessage.class);


            if (parsed.getVerification() == null) {
                parsed.setVerification(new VerifyResponse());
            }

            VerifyResponse vr = parsed.getVerification();


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
            if (parsed.getIntent() == null) parsed.setIntent("CHAT");
            if (parsed.getSessionId() == null) parsed.setSessionId("");

            return parsed;

        } catch (Exception e) {

            return ChatMessage.builder()
                    .sessionId("")
                    .intent("CHAT")
                    .reply("Sorry, I couldn't generate a clean response right now.")
                    .verification(
                            VerifyResponse.builder()
                                    .verdict("UNVERIFIED")
                                    .fake_probability(40)
                                    .true_probability(60)
                                    .reasoning("Could not parse model response.")
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
//public class NormalChatService {
//    private final OllamaLLMService llmService;
//
//    private final ObjectMapper mapper = new ObjectMapper();
//
//    public NormalChatService(OllamaLLMService llmService) {
//        this.llmService = llmService;
//    }
//
//    public ChatMessage chat(String userRequest, String intent) {
//
//        String prompt = buildPrompt(userRequest, intent);
//        String raw = llmService.generateJson(prompt).trim();
//
//        return safeParse(raw);
//    }
//
//    private String buildPrompt(String userRequest, String intent) {
//
//        StringBuilder sb = new StringBuilder();
//
//        sb.append("""
//You are a News Agent for chatting with the user normally.
//
//Return STRICT JSON ONLY (no markdown, no extra text).
//
//Rules:
//- intent must be exactly: CHAT or VERIFY
//- sessionId must be a short string (you may generate one)
//- reply: should be a natural helpful reply to the user (max 4-5 lines)
//
//
//User Request:
//""").append(userRequest).append("\n\n");
//
//        sb.append("User intent:\n").append(intent).append("\n\n");
//
//        sb.append("""
//Return JSON in EXACTLY this format:
//{
//  "sessionId": "string",
//  "intent": "CHAT or VERIFY",
//  "reply": "string",
//  "verification": null
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
//    private ChatMessage safeParse(String raw) {
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
