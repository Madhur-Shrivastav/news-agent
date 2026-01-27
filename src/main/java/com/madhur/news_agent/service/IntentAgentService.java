package com.madhur.news_agent.service;

import com.madhur.news_agent.intents.IntentType;
import org.springframework.stereotype.Service;

@Service
public class IntentAgentService {

    private final GeminiLLMService llmService;

    public IntentAgentService(GeminiLLMService llmService) {
        this.llmService = llmService;
    }

    public IntentType detectIntent(String message) {

        String prompt = """
You are an intent classifier for a news chat bot.

Classify the user message into exactly ONE label:
VERIFY  -> user wants fact-checking / verification of a claim
CHAT    -> casual/general chat not requiring verification

Rules:
- Return ONLY one word: VERIFY or CHAT
- No punctuation, no explanation, no extra words

Message:
""" + message;

        String out = llmService.generateText(prompt)
                .trim()
                .toUpperCase();

        String firstToken = out.split("\\s+")[0];

        if ("VERIFY".equals(firstToken)) return IntentType.VERIFY;
        return IntentType.CHAT;
    }

}

//package com.madhur.news_agent.service;
//
//import com.madhur.news_agent.agents.IntentType;
//import org.springframework.stereotype.Service;
//
//@Service
//public class IntentAgentService {
//
//    private final OllamaLLMService llmService;
//
//    public IntentAgentService(OllamaLLMService llmService) {
//        this.llmService = llmService;
//    }
//
//    public IntentType detectIntent(String message) {
//        if (isVerification(message)) {
//            return IntentType.VERIFY;
//        }
//
//        String prompt = """
//You are an intent classifier for a news chatbot.
//
//Classify the user message into exactly ONE label:
//VERIFY  -> user wants fact-checking / verification of a claim
//CHAT    -> casual/general chat not requiring verification
//
//Rules:
//- Return ONLY one word: VERIFY or CHAT
//- No punctuation, no explanation, no extra words
//
//Message:
//""" + message;
//
//        String out = llmService.generateText(prompt)
//                .trim()
//                .toUpperCase();
//
//        String firstToken = out.split("\\s+")[0];
//
//        if ("VERIFY".equals(firstToken)) return IntentType.VERIFY;
//        return IntentType.CHAT;
//    }
//
//    private boolean isVerification(String msg) {
//        String m = msg.toLowerCase().trim();
//
//        // typical verification patterns
//        return m.startsWith("did ")
//                || m.startsWith("has ")
//                || m.startsWith("is ")
//                || m.contains("is it true")
//                || m.contains("is this true")
//                || m.contains("fact check")
//                || m.contains("verify")
//                || m.contains("fake")
//                || m.contains("real")
//                || m.contains("hoax")
//                || m.contains("rumor")
//                || m.contains("claimed")
//                || m.contains("announced")
//                || m.contains("according to")
//                || m.contains("report says")
//                || m.contains("news says");
//    }
//
//}
