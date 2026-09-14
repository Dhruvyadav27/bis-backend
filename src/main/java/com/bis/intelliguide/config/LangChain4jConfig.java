package com.bis.intelliguide.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM + embeddings both via Gemini / Google AI Studio — same API key for both,
 * free tier, no GCP project/service-account needed.
 * Get a key at https://ai.google.dev/gemini-api/docs/api-key
 */
@Configuration
public class LangChain4jConfig {

    @Value("${app.llm.api-key}")
    private String llmApiKey;

    @Value("${app.llm.model}")
    private String llmModel;

    @Value("${app.embedding.api-key}")
    private String embeddingApiKey;

    @Value("${app.embedding.model}")
    private String embeddingModel;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(llmApiKey)
                .modelName(llmModel)
                .temperature(0.2)
                .timeout(java.time.Duration.ofSeconds(90))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(embeddingApiKey)
                .modelName(embeddingModel)
                .timeout(java.time.Duration.ofSeconds(90))
                .build();
    }
}