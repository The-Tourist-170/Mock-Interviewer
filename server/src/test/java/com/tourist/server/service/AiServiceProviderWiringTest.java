package com.tourist.server.service;

import com.tourist.server.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AiServiceProviderWiringTest {

    @Configuration
    static class TestObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner(
            AnnotationConfigApplicationContext::new)
            .withUserConfiguration(AppConfig.class, TestObjectMapperConfig.class,
                    GeminiService.class, OpenAiCompatibleService.class);

    @Test
    void defaultProvider_loadsGeminiService() {
        contextRunner
                .withPropertyValues(
                        "ai.provider=gemini",
                        "gemini.api-key=test-key",
                        "gemini.model-name=test-model",
                        "ai.base-url=https://api.openai.com/v1",
                        "ai.api-key=test-key",
                        "ai.model=gpt-4o")
                .run(context -> {
                    assertThat(context).hasSingleBean(GeminiService.class);
                    assertThat(context).doesNotHaveBean(OpenAiCompatibleService.class);
                    assertThat(context.getBean(AiService.class)).isInstanceOf(GeminiService.class);
                });
    }

    @Test
    void geminiProvider_loadsGeminiService() {
        contextRunner
                .withPropertyValues(
                        "ai.provider=gemini",
                        "gemini.api-key=test-key",
                        "gemini.model-name=test-model",
                        "ai.base-url=https://api.openai.com/v1",
                        "ai.api-key=test-key",
                        "ai.model=gpt-4o")
                .run(context -> {
                    assertThat(context).hasSingleBean(GeminiService.class);
                    assertThat(context).doesNotHaveBean(OpenAiCompatibleService.class);
                    assertThat(context.getBean(AiService.class)).isInstanceOf(GeminiService.class);
                });
    }

    @Test
    void openaiProvider_loadsOpenAiCompatibleService() {
        contextRunner
                .withPropertyValues(
                        "ai.provider=openai",
                        "gemini.api-key=test-key",
                        "gemini.model-name=test-model",
                        "ai.base-url=https://api.openai.com/v1",
                        "ai.api-key=test-key",
                        "ai.model=gpt-4o")
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenAiCompatibleService.class);
                    assertThat(context).doesNotHaveBean(GeminiService.class);
                    assertThat(context.getBean(AiService.class)).isInstanceOf(OpenAiCompatibleService.class);
                });
    }

    @Test
    void geminiServiceHasMatchIfMissingAnnotation() {
        ConditionalOnProperty annotation = GeminiService.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.matchIfMissing()).isTrue();
    }

    @Test
    void openAiServiceDoesNotMatchIfMissing() {
        ConditionalOnProperty annotation = OpenAiCompatibleService.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.matchIfMissing()).isFalse();
    }
}
