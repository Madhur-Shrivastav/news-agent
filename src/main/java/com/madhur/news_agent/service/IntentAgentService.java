package com.madhur.news_agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madhur.news_agent.dto.IntentResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntentAgentService {

    private final GeminiLLMService llmService;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String[] COLLECTIONS = {
            "apple","microsoft","google","openai","meta","amazon","tesla",
            "nvidia","intel","samsung","tiktok","youtube","netflix","spotify",
            "twitter","x",
    };

    public IntentAgentService(GeminiLLMService llmService) {
        this.llmService = llmService;
    }

    public IntentResponse detectIntent(String message) {

        String collectionsList = String.join(", ", COLLECTIONS);

        String prompt = """
                  You are an intelligent news intent classifier and retrieval optimizer.
                  
                  Your tasks:
                  
                  1️⃣ Classify intent:
                  VERIFY → user wants fact-checking or verification
                  CHAT → general conversation
                  
                  2️⃣ Rewrite the user message into an optimized semantic search query.
                     - Remove conversational words.
                     - Preserve factual meaning.
                     - Add relevant context terms for news retrieval.
                     - For geopolitical or conflict topics, include terms like:
                       military, tensions, government statement, defense, conflict, escalation.
                  
                  3️⃣ Select the BEST MongoDB collection from the list.
                  
                  4️⃣ Extract canonical named entities (nouns only).
                     Entities can include:
                     - Countries
                     - People
                     - Organizations
                     - Companies
                     - Locations
                     - Products
                  
                     Rules:
                     - Convert adjectives to nouns (Ukrainian → Ukraine).
                     - No duplicates.
                     - Lowercase preferred.
                  
                  5️⃣ Extract keyword tokens for database search.
                     Rules:
                     - Keywords must NOT repeat entities.
                     - Use meaningful news vocabulary only.
                     - Avoid slang (e.g., nukes).
                     - Use single-word tokens when possible.
                     - Split phrases into tokens (nuclear war → nuclear, war).
                     - Include debunk-related terms if claim may be misinformation:
                       rumor, hoax, false, claim, report, denial, verification.
                     - Prefer domain-specific keywords based on the entities context.
                       Examples:
                       Sports → cricket, match, player, team, tournament
                       Entertainment → film, actor, instagram, viral, celebrity
                       Politics → election, government, policy, statement
                     - Avoid generic words like: news, report, claim, information.
                     
                  
                  Return STRICT JSON ONLY:
                  
                  {
                    "intent": "VERIFY or CHAT",
                    "optimizedQuery": "rewritten semantic query",
                    "collection": "collection_name",
                    "entities": ["entity1","entity2"],
                    "keywords": ["token1","token2"]
                  }
                  
                  Rules:
                  - No explanation
                  - No markdown
                  - No extra text
                  - Arrays must always be present (can be empty)
                  
                              
                User message:
                """ + message;

        try {

            String output = llmService.generateText(prompt);


            output = output.replace("```json", "")
                    .replace("```", "")
                    .trim();

            IntentResponse response = mapper.readValue(output, IntentResponse.class);

            if (response.getEntities() == null)
                response.setEntities(List.of());

            if (response.getCollection() == null || response.getCollection().isBlank())
                response.setCollection("general");

            if (response.getIntent() == null)
                response.setIntent("CHAT");

            if (response.getOptimizedQuery() == null || response.getOptimizedQuery().isBlank())
                response.setOptimizedQuery(message);

            return response;

        } catch (Exception e) {


            return new IntentResponse(
                    "CHAT",
                    message,
                    "general",
                    List.of(),
                    List.of()
            );
        }
    }
}
