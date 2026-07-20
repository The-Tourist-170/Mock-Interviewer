package com.tourist.server.service;

import com.tourist.server.TestcontainersConfiguration;
import com.tourist.server.dto.AnswerDTO;
import com.tourist.server.dto.CandidateDetailDTO;
import com.tourist.server.dto.InterviewStateDTO;
import com.tourist.server.dto.ResumeAnalysisDTO;
import com.tourist.server.model.InterviewStatus;
import com.tourist.server.repository.CandidateRepository;
import com.tourist.server.repository.InterviewSessionRepository;
import com.tourist.server.service.InterviewService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class InterviewFlowIntegrationTest {

    @TempDir
    static Path tempUploadDir;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("file.upload-dir", () -> tempUploadDir.toString());
    }

    @Autowired
    private InterviewService interviewService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @MockBean
    private AiService aiService;

    private static final String VALID_QUESTIONS =
            "EASY: Tell me about your React experience.\n" +
            "MEDIUM: Explain how you would design a database schema for a multi-tenant app.\n" +
            "HARD: Describe a time you resolved a critical production incident.\n" +
            "EASY: What testing frameworks have you used?\n" +
            "MEDIUM: How do you handle state management in a large React application?\n" +
            "HARD: Walk me through architecting a real-time collaborative editing feature.";

    private static final String VALID_EVALUATION = "SCORE: 8\nFEEDBACK: Good answer with solid reasoning.";

    private static final String VALID_RESUME_ANALYSIS_JSON =
            "{\"strengths\":[\"React\",\"Node.js\",\"TypeScript\"]," +
            "\"weaknesses\":[\"Database design\",\"DevOps\"]," +
            "\"recommendation\":{\"verdict\":\"Recommended\",\"rationale\":\"Strong frontend skills with growth potential.\"}," +
            "\"graph_data\":{\"skill_ratings\":{\"React\":9,\"Node.js\":7,\"TypeScript\":8}," +
            "\"experience_breakdown\":{\"Frontend\":60,\"Backend\":30,\"DevOps\":10}}}";

    private static final String VALID_SUMMARY = "Candidate showed strong React and Node.js knowledge.";

    @BeforeEach
    void setUpMocks() {
        when(aiService.analyzeResume(anyString())).thenReturn(VALID_RESUME_ANALYSIS_JSON);
        when(aiService.generateQuestions(any(), any())).thenReturn(VALID_QUESTIONS);
        when(aiService.evaluateAnswer(anyString(), anyString())).thenReturn(VALID_EVALUATION);
        when(aiService.summarizePerformance(any())).thenReturn(VALID_SUMMARY);
    }

    @AfterEach
    void cleanRepositories() {
        interviewSessionRepository.deleteAll();
        candidateRepository.deleteAll();
    }

    @Test
    void fullInterviewFlow_completesSession_andSetsCandidateScore() throws IOException {
        MultipartFile resume = createPdfResume();
        InterviewStateDTO initialState = interviewService.startInterview("Alice", "alice@example.com", "555-0100", resume);

        assertThat(initialState.status()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(initialState.totalQuestions()).isEqualTo(6);
        assertThat(initialState.currentQuestionIndex()).isEqualTo(0);
        assertThat(initialState.candidateName()).isEqualTo("Alice");
        assertThat(initialState.currentQuestionText()).isEqualTo("Tell me about your React experience.");

        UUID sessionId = initialState.sessionId();

        InterviewStateDTO state = initialState;
        for (int i = 0; i < 5; i++) {
            state = interviewService.submitAnswer(sessionId, new AnswerDTO("My answer to question " + (i + 1)));
            assertThat(state.status()).isEqualTo(InterviewStatus.IN_PROGRESS);
            assertThat(state.currentQuestionIndex()).isEqualTo(i + 1);
        }

        InterviewStateDTO finalState = interviewService.submitAnswer(sessionId, new AnswerDTO("My final answer."));
        assertThat(finalState.status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(finalState.currentQuestionText()).isEqualTo("Interview Complete!");
        assertThat(finalState.timer()).isZero();

        UUID candidateId = candidateRepository.findAll().stream()
                .filter(c -> c.getName().equals("Alice"))
                .findFirst()
                .map(c -> c.getId())
                .orElseThrow();
        CandidateDetailDTO detail = interviewService.getCandidateDetails(candidateId);

        assertThat(detail.name()).isEqualTo("Alice");
        assertThat(detail.email()).isEqualTo("alice@example.com");
        assertThat(detail.score()).isEqualTo(8.0);
        assertThat(detail.summary()).isEqualTo(VALID_SUMMARY);
        assertThat(detail.strengths()).containsExactly("React", "Node.js", "TypeScript");
        assertThat(detail.weaknesses()).containsExactly("Database design", "DevOps");
        assertThat(detail.recommendationVerdict()).isEqualTo("Recommended");
        assertThat(detail.skillRatings()).containsEntry("React", 9);
        assertThat(detail.experienceBreakdown()).containsEntry("Frontend", 60);
        assertThat(detail.questions()).hasSize(6);
        assertThat(detail.questions().get(0).candidateAnswer()).isEqualTo("My answer to question 1");
        assertThat(detail.questions().get(0).aiScore()).isEqualTo(8);
    }

    @Test
    void fullInterviewFlow_jsonbColumnsPersistAndRoundTrip() throws IOException {
        MultipartFile resume = createPdfResume();
        InterviewStateDTO initialState = interviewService.startInterview("Bob", "bob@example.com", "555-0200", resume);
        UUID sessionId = initialState.sessionId();

        for (int i = 0; i < 6; i++) {
            interviewService.submitAnswer(sessionId, new AnswerDTO("Answer " + (i + 1)));
        }

        UUID candidateId = candidateRepository.findAll().stream()
                .filter(c -> c.getName().equals("Bob"))
                .findFirst()
                .map(c -> c.getId())
                .orElseThrow();

        CandidateDetailDTO detail = interviewService.getCandidateDetails(candidateId);

        assertThat(detail.strengths()).isNotNull().hasSize(3);
        assertThat(detail.weaknesses()).isNotNull().hasSize(2);
        assertThat(detail.skillRatings()).isNotNull().hasSize(3);
        assertThat(detail.experienceBreakdown()).isNotNull().hasSize(3);
        assertThat(detail.skillRatings().get("Node.js")).isEqualTo(7);
        assertThat(detail.experienceBreakdown().get("DevOps")).isEqualTo(10);
    }

    @Test
    void emptyQuestionsResponse_throwsRuntimeException() throws IOException {
        when(aiService.generateQuestions(any(), any())).thenReturn("Sorry, I cannot generate questions right now.");

        MultipartFile resume = createPdfResume();

        assertThatThrownBy(() -> interviewService.startInterview("Carol", "carol@example.com", "555-0300", resume))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate interview questions");
    }

    @Test
    void findByCandidateId_returnsCorrectSession() throws IOException {
        MultipartFile resume = createPdfResume();
        InterviewStateDTO state = interviewService.startInterview("Dave", "dave@example.com", "555-0400", resume);

        UUID candidateId = candidateRepository.findAll().stream()
                .filter(c -> c.getName().equals("Dave"))
                .findFirst()
                .map(c -> c.getId())
                .orElseThrow();

        CandidateDetailDTO detail = interviewService.getCandidateDetails(candidateId);

        assertThat(detail.name()).isEqualTo("Dave");
        assertThat(detail.questions()).hasSize(6);
        assertThat(detail.questions().get(0).questionText()).isEqualTo("Tell me about your React experience.");
        assertThat(detail.questions().get(0).difficulty().name()).isEqualTo("EASY");
        assertThat(detail.questions().get(2).difficulty().name()).isEqualTo("HARD");
    }

    @Test
    void resumeAnalysisFailure_fallsBackGracefully_andPassesNullToGenerateQuestions() throws IOException {
        when(aiService.analyzeResume(anyString())).thenThrow(new RuntimeException("Gemini is down"));
        when(aiService.generateQuestions(any(), any())).thenAnswer(invocation -> {
            ResumeAnalysisDTO passedAnalysis = invocation.getArgument(1);
            assertThat(passedAnalysis).isNull();
            return VALID_QUESTIONS;
        });

        MultipartFile resume = createPdfResume();
        InterviewStateDTO state = interviewService.startInterview("Eve", "eve@example.com", "555-0500", resume);

        assertThat(state.status()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(state.totalQuestions()).isEqualTo(6);

        UUID candidateId = candidateRepository.findAll().stream()
                .filter(c -> c.getName().equals("Eve"))
                .findFirst()
                .map(c -> c.getId())
                .orElseThrow();

        CandidateDetailDTO detail = interviewService.getCandidateDetails(candidateId);
        assertThat(detail.strengths()).containsExactly("Analysis Failed");
        assertThat(detail.weaknesses()).containsExactly("Analysis Failed");
        assertThat(detail.recommendationVerdict()).isEqualTo("Error");
    }

    @Test
    void timerValues_matchDifficultyAcrossFlow() throws IOException {
        MultipartFile resume = createPdfResume();
        InterviewStateDTO state = interviewService.startInterview("Frank", "frank@example.com", "555-0600", resume);
        UUID sessionId = state.sessionId();

        assertThat(state.timer()).isEqualTo(20);

        state = interviewService.submitAnswer(sessionId, new AnswerDTO("answer 1"));
        assertThat(state.timer()).isEqualTo(60);

        state = interviewService.submitAnswer(sessionId, new AnswerDTO("answer 2"));
        assertThat(state.timer()).isEqualTo(120);

        state = interviewService.submitAnswer(sessionId, new AnswerDTO("answer 3"));
        assertThat(state.timer()).isEqualTo(20);
    }

    @Test
    void submitAnswer_nonExistentSession_throwsResourceNotFound() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> interviewService.submitAnswer(randomId, new AnswerDTO("answer")))
                .isInstanceOf(com.tourist.server.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Interview session not found");
    }

    @Test
    void getCandidateDetails_nonExistentCandidate_throwsResourceNotFound() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> interviewService.getCandidateDetails(randomId))
                .isInstanceOf(com.tourist.server.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Candidate not found");
    }

    private MultipartFile createPdfResume() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(baos);
        }
        byte[] pdfBytes = baos.toByteArray();
        Loader.loadPDF(pdfBytes).close();
        return new MockMultipartFile("resume", "resume.pdf", "application/pdf", pdfBytes);
    }
}
