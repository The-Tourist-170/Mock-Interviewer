package com.tourist.server.service;

import com.tourist.server.dto.GeminiRequest;
import com.tourist.server.dto.GeminiResponse;
import com.tourist.server.dto.ResumeAnalysisDTO;
import com.tourist.server.model.Question;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiService implements AiService {

        private final RestTemplate restTemplate;
        private final String apiUrl;
        private final String modelName;

        public GeminiService(RestTemplate restTemplate,
                        @Value("${gemini.api-key}") String apiKey,
                        @Value("${gemini.model-name}") String modelName) {
                this.restTemplate = restTemplate;
                this.modelName = modelName;
                this.apiUrl = UriComponentsBuilder
                                .fromUriString("https://generativelanguage.googleapis.com/v1beta/models/")
                                .path(modelName + ":generateContent")
                                .queryParam("key", apiKey)
                                .toUriString();
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
                GeminiRequest.Part part = new GeminiRequest.Part(prompt);
                GeminiRequest.Content content = new GeminiRequest.Content(List.of(part));
                GeminiRequest request = new GeminiRequest(List.of(content));

                GeminiResponse response = restTemplate.postForObject(apiUrl, request, GeminiResponse.class);

                if (response != null) {
                        return response.text();
                }
                throw new RuntimeException("Failed to get response from Gemini API");
        }
}
