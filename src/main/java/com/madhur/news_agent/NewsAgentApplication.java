package com.madhur.news_agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewsAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(NewsAgentApplication.class, args);

	}

}
