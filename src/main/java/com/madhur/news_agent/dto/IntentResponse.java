package com.madhur.news_agent.dto;

import com.madhur.news_agent.intents.IntentType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentResponse {
    private IntentType intent;
    private String claim;

}
