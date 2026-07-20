package com.tourist.server.service;

import com.tourist.server.dto.GeminiRequest;
import com.tourist.server.dto.GeminiResponse;
import com.tourist.server.dto.ResumeAnalysisDTO;
import com.tourist.server.model.Question;
import com.tourist.server.model.QuestionDifficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GeminiService geminiService;

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(restTemplate, "fake-api-key", "gemini-1.5-flash");
    }

    @Test
    void generateQuestions_withoutPreviousQuestions_callsApiAndReturnsText() {
        GeminiResponse mockResponse = createGeminiResponse("EASY: What is React?\nHARD: Explain event loop.");
        when(restTemplate.postForObject(any(String.class), any(GeminiRequest.class), eq(GeminiResponse.class)))
                .thenReturn(mockResponse);

        String result = geminiService.generateQuestions(null, null);

        assertThat(result).isEqualTo("EASY: What is React?\nHARD: Explain event loop.");
    }

    @Test
    void generateQuestions_withResumeAnalysis_promptContainsResumeContext() {
        ResumeAnalysisDTO analysis = new ResumeAnalysisDTO(
                List.of("React", "TypeScript"),
                List.of("Database design"),
                new ResumeAnalysisDTO.Recommendation("Recommended", "Strong candidate"),
                new ResumeAnalysisDTO.GraphData(
                        Map.of("React", 8, "TypeScript", 7),
                        Map.of("Frontend", 70, "Backend", 30)));

        when(restTemplate.postForObject(any(String.class), any(GeminiRequest.class), eq(GeminiResponse.class)))
                .thenReturn(createGeminiResponse("EASY: q1"));

        geminiService.generateQuestions(null, analysis);

        ArgumentCaptor<GeminiRequest> captor = ArgumentCaptor.forClass(GeminiRequest.class);
        org.mockito.Mockito.verify(restTemplate).postForObject(any(String.class), captor.capture(), eq(GeminiResponse.class));
        String prompt = captor.getValue().contents().get(0).parts().get(0).text();
        assertThat(prompt).contains("React", "TypeScript", "Database design");
        assertThat(prompt).contains("Strengths:", "Weaknesses:", "Skill ratings:", "Experience:");
    }

    @Test
    void generateQuestions_withPreviousQuestions_promptContainsAvoidanceContext() {
        Question prevQ = new Question();
        prevQ.setQuestionText("What is a Promise?");
        prevQ.setDifficulty(QuestionDifficulty.EASY);

        when(restTemplate.postForObject(any(String.class), any(GeminiRequest.class), eq(GeminiResponse.class)))
                .thenReturn(createGeminiResponse("EASY: q1"));

        geminiService.generateQuestions(List.of(prevQ), null);

        ArgumentCaptor<GeminiRequest> captor = ArgumentCaptor.forClass(GeminiRequest.class);
        org.mockito.Mockito.verify(restTemplate).postForObject(any(String.class), captor.capture(), eq(GeminiResponse.class));
        String prompt = captor.getValue().contents().get(0).parts().get(0).text();
        assertThat(prompt).contains("What is a Promise?");
        assertThat(prompt).contains("avoid repeating");
    }

    @Test
    void evaluateAnswer_callsApiAndReturnsText() {
        when(restTemplate.postForObject(any(String.class), any(GeminiRequest.class), eq(GeminiResponse.class)))
                .thenReturn(createGeminiResponse("SCORE: 8\nFEEDBACK: Good answer"));

        String result = geminiService.evaluateAnswer("What is React?", "A UI library");

        assertThat(result).isEqualTo("SCORE: 8\nFEEDBACK: Good answer");
    }

    @Test
    void analyzeResume_callsApiAndReturnsText() {
        when(restTemplate.postForObject(any(String.class), any(GeminiRequest.class), eq(GeminiResponse.class)))
                .thenReturn(createGeminiResponse("{\"strengths\":[\"React\"]}"));

        String result = geminiService.analyzeResume("resume text here");

        assertThat(result).isEqualTo("{\"strengths\":[\"React\"]}");
    }

    @Test
    void summarizePerformance_callsApiAndReturnsText() {
        Question q = new Question();
        q.setQuestionText("Q1");
        q.setCandidateAnswer("A1");
        q.setAiScore(7);
        q.setAiFeedback("Good");

        when(restTemplate.postForObject(any(String.class), any(GeminiRequest.class), eq(GeminiResponse.class)))
                .thenReturn(createGeminiResponse("Candidate performed well"));

        String result = geminiService.summarizePerformance(List.of(q));

        assertThat(result).isEqualTo("Candidate performed well");
    }

    @Test
    void extractInfoFromResume_callsApiAndReturnsText() {
        when(restTemplate.postForObject(any(String.class), any(GeminiRequest.class), eq(GeminiResponse.class)))
                .thenReturn(createGeminiResponse("{\"name\":\"John\"}"));

        String result = geminiService.extractInfoFromResume("resume text");

        assertThat(result).isEqualTo("{\"name\":\"John\"}");
    }

    @Test
    void generateText_nullResponse_throws() {
        when(restTemplate.postForObject(any(String.class), any(GeminiRequest.class), eq(GeminiResponse.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> geminiService.evaluateAnswer("Q", "A"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get response from Gemini API");
    }

    private GeminiResponse createGeminiResponse(String text) {
        GeminiResponse.Part part = new GeminiResponse.Part(text);
        GeminiResponse.Content content = new GeminiResponse.Content(List.of(part));
        GeminiResponse.Candidate candidate = new GeminiResponse.Candidate(content);
        return new GeminiResponse(List.of(candidate));
    }
}
