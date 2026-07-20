package com.tourist.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourist.server.dto.OpenAiRequest;
import com.tourist.server.dto.ResumeAnalysisDTO;
import com.tourist.server.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiCompatibleServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OpenAiCompatibleService service;

    @BeforeEach
    void setUp() {
        service = new OpenAiCompatibleService(restTemplate, objectMapper,
                "https://api.openai.com/v1", "fake-key", "gpt-4o");
    }

    @Test
    void constructor_baseUrlWithTrailingSlash_appendsCorrectly() {
        OpenAiCompatibleService s = new OpenAiCompatibleService(restTemplate, objectMapper,
                "https://api.example.com/v1/", "key", "model");
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse("result"), HttpStatus.OK));

        s.evaluateAnswer("Q", "A");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(restTemplate).postForEntity(urlCaptor.capture(), any(), eq(String.class));
        assertThat(urlCaptor.getValue()).isEqualTo("https://api.example.com/v1/chat/completions");
    }

    @Test
    void generateQuestions_withoutResumeAnalysis_callsApiAndReturnsContent() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse("EASY: What is React?"), HttpStatus.OK));

        String result = service.generateQuestions(null, null);

        assertThat(result).isEqualTo("EASY: What is React?");
    }

    @Test
    void generateQuestions_withResumeAnalysis_promptContainsResumeContext() {
        ResumeAnalysisDTO analysis = new ResumeAnalysisDTO(
                List.of("React", "Node.js"),
                List.of("CSS"),
                new ResumeAnalysisDTO.Recommendation("Recommended", "Good"),
                new ResumeAnalysisDTO.GraphData(
                        Map.of("React", 9),
                        Map.of("Frontend", 60, "Backend", 40)));

        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse("EASY: q"), HttpStatus.OK));

        service.generateQuestions(null, analysis);

        ArgumentCaptor<HttpEntity<OpenAiRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).postForEntity(any(String.class), captor.capture(), eq(String.class));
        OpenAiRequest req = captor.getValue().getBody();
        assertThat(req).isNotNull();
        String prompt = req.messages().get(0).content();
        assertThat(prompt).contains("React", "Node.js", "CSS");
        assertThat(prompt).contains("Strengths:", "Weaknesses:");
    }

    @Test
    void evaluateAnswer_buildsCorrectRequest() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse("SCORE: 7\nFEEDBACK: Decent"), HttpStatus.OK));

        String result = service.evaluateAnswer("What is a closure?", "A function with access to outer scope");

        assertThat(result).isEqualTo("SCORE: 7\nFEEDBACK: Decent");
    }

    @Test
    void analyzeResume_returnsContent() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse("{\"strengths\":[\"Go\"]}"), HttpStatus.OK));

        String result = service.analyzeResume("resume text");

        assertThat(result).isEqualTo("{\"strengths\":[\"Go\"]}");
    }

    @Test
    void summarizePerformance_returnsContent() {
        Question q = new Question();
        q.setQuestionText("Q1");
        q.setCandidateAnswer("A1");
        q.setAiScore(6);
        q.setAiFeedback("OK");

        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse("Average performance"), HttpStatus.OK));

        String result = service.summarizePerformance(List.of(q));

        assertThat(result).isEqualTo("Average performance");
    }

    @Test
    void extractInfoFromResume_returnsContent() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse("{\"name\":\"Jane\"}"), HttpStatus.OK));

        String result = service.extractInfoFromResume("resume text");

        assertThat(result).isEqualTo("{\"name\":\"Jane\"}");
    }

    @Test
    void generateText_non2xxResponse_throwsWithBody() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Not authorized", HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> service.evaluateAnswer("Q", "A"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("401")
                .hasMessageContaining("Not authorized");
    }

    @Test
    void generateText_nullBody_throws() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThatThrownBy(() -> service.evaluateAnswer("Q", "A"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("HTTP 200");
    }

    @Test
    void generateText_nonJsonBody_throwsWithBody() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Not Found", HttpStatus.OK));

        assertThatThrownBy(() -> service.evaluateAnswer("Q", "A"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("non-JSON")
                .hasMessageContaining("Not Found");
    }

    @Test
    void generateText_emptyChoices_returnsNoContentError() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"choices\":[]}", HttpStatus.OK));

        assertThatThrownBy(() -> service.evaluateAnswer("Q", "A"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no content");
    }

    @Test
    void apiUrl_usesChatCompletionsEndpoint() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse("ok"), HttpStatus.OK));

        service.evaluateAnswer("Q", "A");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(restTemplate).postForEntity(urlCaptor.capture(), any(), eq(String.class));
        assertThat(urlCaptor.getValue()).isEqualTo("https://api.openai.com/v1/chat/completions");
    }

    @Test
    void request_includesBearerAuthHeader() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse("ok"), HttpStatus.OK));

        service.evaluateAnswer("Q", "A");

        ArgumentCaptor<HttpEntity<OpenAiRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate).postForEntity(any(String.class), captor.capture(), eq(String.class));
        HttpHeaders headers = captor.getValue().getHeaders();
        assertThat(headers.get("Authorization")).containsExactly("Bearer fake-key");
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    private String jsonResponse(String content) {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                + objectMapper.valueToTree(content).toString() + "}}]}";
    }
}
