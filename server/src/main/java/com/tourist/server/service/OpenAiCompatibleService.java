package com.tourist.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourist.server.dto.OpenAiRequest;
import com.tourist.server.dto.OpenAiResponse;
import com.tourist.server.dto.ResumeAnalysisDTO;
import com.tourist.server.model.Question;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai")
public class OpenAiCompatibleService implements AiService {

        private final RestTemplate restTemplate;
        private final ObjectMapper objectMapper;
        private final String apiUrl;
        private final String apiKey;
        private final String model;

        public OpenAiCompatibleService(
                        RestTemplate restTemplate,
                        ObjectMapper objectMapper,
                        @Value("${ai.base-url}") String baseUrl,
                        @Value("${ai.api-key}") String apiKey,
                        @Value("${ai.model}") String model) {
                this.restTemplate = restTemplate;
                this.objectMapper = objectMapper;
                this.apiKey = apiKey;
                this.model = model;
                String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
                this.apiUrl = base + "/chat/completions";
        }

        @Override
        public String generateQuestions(List<Question> previousQuestions, ResumeAnalysisDTO resumeAnalysis) {
                return generateText(AiPrompts.generateQuestionsPrompt(previousQuestions, resumeAnalysis));
        }

        @Override
        public String extractInfoFromResume(String resumeText) {
                return generateText(AiPrompts.extractInfoFromResumePrompt(resumeText));
        }

        @Override
        public String evaluateAnswer(String question, String answer) {
                return generateText(AiPrompts.evaluateAnswerPrompt(question, answer));
        }

        @Override
        public String analyzeResume(String resumeText) {
                return generateText(AiPrompts.analyzeResumePrompt(resumeText));
        }

        @Override
        public String summarizePerformance(List<Question> questions) {
                return generateText(AiPrompts.summarizePerformancePrompt(questions));
        }

        private String generateText(String prompt) {
                OpenAiRequest.Message message = new OpenAiRequest.Message("user", prompt);
                OpenAiRequest request = new OpenAiRequest(model, List.of(message));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                HttpEntity<OpenAiRequest> entity = new HttpEntity<>(request, headers);

                ResponseEntity<String> responseEntity = restTemplate.postForEntity(apiUrl, entity, String.class);

                if (!responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                        HttpStatusCode status = responseEntity.getStatusCode();
                        String body = responseEntity.getBody();
                        throw new RuntimeException("AI API returned HTTP " + status.value() + ": " + body);
                }

                String body = responseEntity.getBody();
                try {
                        OpenAiResponse response = objectMapper.readValue(body, OpenAiResponse.class);
                        String content = response.content();
                        if (content != null) {
                                return content;
                        }
                        throw new RuntimeException("AI API returned no content. Body: " + body);
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        throw new RuntimeException("AI API returned non-JSON response (HTTP " + responseEntity.getStatusCode().value() + "). Body: " + body, e);
                }
        }
}
