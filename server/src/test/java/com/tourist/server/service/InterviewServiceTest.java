package com.tourist.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourist.server.dto.AnswerDTO;
import com.tourist.server.dto.InterviewStateDTO;
import com.tourist.server.dto.ResumeAnalysisDTO;
import com.tourist.server.exception.ResourceNotFoundException;
import com.tourist.server.model.*;
import com.tourist.server.repository.CandidateRepository;
import com.tourist.server.repository.InterviewSessionRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private CandidateRepository candidateRepository;
    @Mock
    private InterviewSessionRepository interviewSessionRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private AiService aiService;
    @Mock
    private ObjectMapper objectMapper;

    private InterviewService interviewService;
    private byte[] testPdfBytes;

    @BeforeEach
    void setUp() throws IOException {
        interviewService = new InterviewService(candidateRepository, interviewSessionRepository,
                fileStorageService, aiService, objectMapper);
        testPdfBytes = createTestPdf();
    }

    @Test
    void startInterview_success_returnsCorrectState() throws IOException {
        when(fileStorageService.storeFile(any())).thenReturn("/uploads/test.pdf");
        when(aiService.analyzeResume(anyString())).thenReturn("{\"strengths\":[\"React\"]}");
        when(objectMapper.readValue(anyString(), eq(ResumeAnalysisDTO.class)))
                .thenReturn(createMockAnalysis());
        when(interviewSessionRepository.findFirstByOrderByCreationTimestampDesc())
                .thenReturn(Optional.empty());
        when(aiService.generateQuestions(any(), any()))
                .thenReturn("EASY: What is React?\nEASY: What is JSX?\nMEDIUM: Explain hooks\nMEDIUM: Context API\nHARD: Design system\nHARD: Optimize rendering");

        InterviewStateDTO result = interviewService.startInterview("John", "john@test.com", "555-1234",
                createMockResume());

        assertThat(result.status()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(result.totalQuestions()).isEqualTo(6);
        assertThat(result.currentQuestionIndex()).isEqualTo(0);
        assertThat(result.currentQuestionText()).isEqualTo("What is React?");
        assertThat(result.difficulty()).isEqualTo(QuestionDifficulty.EASY);
        assertThat(result.timer()).isEqualTo(20);
    }

    @Test
    void startInterview_mediumDifficulty_returns60sTimer() throws IOException {
        when(fileStorageService.storeFile(any())).thenReturn("/uploads/test.pdf");
        when(aiService.analyzeResume(anyString())).thenReturn("{\"strengths\":[]}");
        when(objectMapper.readValue(anyString(), eq(ResumeAnalysisDTO.class)))
                .thenReturn(createMockAnalysis());
        when(interviewSessionRepository.findFirstByOrderByCreationTimestampDesc())
                .thenReturn(Optional.empty());
        when(aiService.generateQuestions(any(), any()))
                .thenReturn("MEDIUM: Explain hooks\nEASY: q\nEASY: q\nMEDIUM: q\nHARD: q\nHARD: q");

        InterviewStateDTO result = interviewService.startInterview("John", "john@test.com", "555",
                createMockResume());

        assertThat(result.difficulty()).isEqualTo(QuestionDifficulty.MEDIUM);
        assertThat(result.timer()).isEqualTo(60);
    }

    @Test
    void startInterview_hardDifficulty_returns120sTimer() throws IOException {
        when(fileStorageService.storeFile(any())).thenReturn("/uploads/test.pdf");
        when(aiService.analyzeResume(anyString())).thenReturn("{\"strengths\":[]}");
        when(objectMapper.readValue(anyString(), eq(ResumeAnalysisDTO.class)))
                .thenReturn(createMockAnalysis());
        when(interviewSessionRepository.findFirstByOrderByCreationTimestampDesc())
                .thenReturn(Optional.empty());
        when(aiService.generateQuestions(any(), any()))
                .thenReturn("HARD: Design a system\nEASY: q\nEASY: q\nMEDIUM: q\nMEDIUM: q\nHARD: q");

        InterviewStateDTO result = interviewService.startInterview("John", "john@test.com", "555",
                createMockResume());

        assertThat(result.difficulty()).isEqualTo(QuestionDifficulty.HARD);
        assertThat(result.timer()).isEqualTo(120);
    }

    @Test
    void startInterview_unparseableQuestions_throws() throws IOException {
        when(fileStorageService.storeFile(any())).thenReturn("/uploads/test.pdf");
        when(aiService.analyzeResume(anyString())).thenReturn("{\"strengths\":[]}");
        when(objectMapper.readValue(anyString(), eq(ResumeAnalysisDTO.class)))
                .thenReturn(createMockAnalysis());
        when(interviewSessionRepository.findFirstByOrderByCreationTimestampDesc())
                .thenReturn(Optional.empty());
        when(aiService.generateQuestions(any(), any()))
                .thenReturn("This is garbage without proper format");

        assertThatThrownBy(() -> interviewService.startInterview("John", "john@test.com", "555",
                createMockResume()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate interview questions");
    }

    @Test
    void startInterview_resumeAnalysisFails_setsFallbackAndPassesNullAnalysis() throws IOException {
        when(fileStorageService.storeFile(any())).thenReturn("/uploads/test.pdf");
        when(aiService.analyzeResume(anyString())).thenThrow(new RuntimeException("AI error"));
        when(interviewSessionRepository.findFirstByOrderByCreationTimestampDesc())
                .thenReturn(Optional.empty());
        when(aiService.generateQuestions(any(), any()))
                .thenReturn("EASY: q1\nEASY: q2\nMEDIUM: q3\nMEDIUM: q4\nHARD: q5\nHARD: q6");

        interviewService.startInterview("John", "john@test.com", "555", createMockResume());

        ArgumentCaptor<ResumeAnalysisDTO> analysisCaptor = ArgumentCaptor.forClass(ResumeAnalysisDTO.class);
        verify(aiService).generateQuestions(any(), analysisCaptor.capture());
        assertThat(analysisCaptor.getValue()).isNull();
    }

    @Test
    void startInterview_passesResumeAnalysisToGenerateQuestions() throws IOException {
        ResumeAnalysisDTO analysis = createMockAnalysis();
        when(fileStorageService.storeFile(any())).thenReturn("/uploads/test.pdf");
        when(aiService.analyzeResume(anyString())).thenReturn("{\"strengths\":[]}");
        when(objectMapper.readValue(anyString(), eq(ResumeAnalysisDTO.class)))
                .thenReturn(analysis);
        when(interviewSessionRepository.findFirstByOrderByCreationTimestampDesc())
                .thenReturn(Optional.empty());
        when(aiService.generateQuestions(any(), any()))
                .thenReturn("EASY: q1\nEASY: q2\nMEDIUM: q3\nMEDIUM: q4\nHARD: q5\nHARD: q6");

        interviewService.startInterview("John", "john@test.com", "555", createMockResume());

        ArgumentCaptor<ResumeAnalysisDTO> analysisCaptor = ArgumentCaptor.forClass(ResumeAnalysisDTO.class);
        verify(aiService).generateQuestions(any(), analysisCaptor.capture());
        assertThat(analysisCaptor.getValue()).isEqualTo(analysis);
    }

    @Test
    void submitAnswer_success_advancesQuestionIndex() throws IOException {
        InterviewSession session = createSessionWithQuestions(3, 0);
        when(interviewSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(aiService.evaluateAnswer(anyString(), anyString()))
                .thenReturn("SCORE: 8\nFEEDBACK: Good answer");

        InterviewStateDTO result = interviewService.submitAnswer(session.getId(),
                new AnswerDTO("My answer"));

        assertThat(result.currentQuestionIndex()).isEqualTo(1);
        assertThat(result.status()).isEqualTo(InterviewStatus.IN_PROGRESS);
        assertThat(result.difficulty()).isEqualTo(QuestionDifficulty.MEDIUM);
        assertThat(result.timer()).isEqualTo(60);
    }

    @Test
    void submitAnswer_lastQuestion_completesAndCalculatesScore() throws IOException {
        InterviewSession session = createSessionWithQuestions(2, 1);
        session.getQuestions().get(0).setAiScore(9);
        when(interviewSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(aiService.evaluateAnswer(anyString(), anyString()))
                .thenReturn("SCORE: 9\nFEEDBACK: Excellent");
        when(aiService.summarizePerformance(any())).thenReturn("Great candidate");

        InterviewStateDTO result = interviewService.submitAnswer(session.getId(),
                new AnswerDTO("Final answer"));

        assertThat(result.status()).isEqualTo(InterviewStatus.COMPLETED);
        assertThat(result.currentQuestionText()).isEqualTo("Interview Complete!");
        assertThat(result.difficulty()).isNull();
        assertThat(result.timer()).isEqualTo(0);
        assertThat(session.getCandidate().getScore()).isEqualTo(9.0);
        assertThat(session.getCandidate().getSummary()).isEqualTo("Great candidate");
    }

    @Test
    void submitAnswer_sessionNotFound_throws() {
        UUID sessionId = UUID.randomUUID();
        when(interviewSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.submitAnswer(sessionId, new AnswerDTO("answer")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Interview session not found");
    }

    @Test
    void submitAnswer_invalidEvaluationFormat_setsDefaultScore() throws IOException {
        InterviewSession session = createSessionWithQuestions(3, 0);
        when(interviewSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(aiService.evaluateAnswer(anyString(), anyString()))
                .thenReturn("No valid format here");

        interviewService.submitAnswer(session.getId(), new AnswerDTO("answer"));

        Question answeredQuestion = session.getQuestions().get(0);
        assertThat(answeredQuestion.getAiScore()).isEqualTo(0);
        assertThat(answeredQuestion.getAiFeedback()).contains("AI evaluation failed");
    }

    @Test
    void getCandidateDetails_success_returnsAllFields() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = new Candidate();
        candidate.setId(candidateId);
        candidate.setName("Jane");
        candidate.setEmail("jane@test.com");
        candidate.setPhone("555-9999");
        candidate.setScore(8.5);
        candidate.setSummary("Good candidate");
        candidate.setStrengths(List.of("React", "Node"));
        candidate.setWeaknesses(List.of("CSS"));
        candidate.setRecommendationVerdict("Recommended");
        candidate.setRecommendationRationale("Strong");
        candidate.setSkillRatings(Map.of("React", 9));
        candidate.setExperienceBreakdown(Map.of("Frontend", 80, "Backend", 20));

        InterviewSession session = createSessionWithQuestions(2, 0);
        session.setCandidate(candidate);

        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(interviewSessionRepository.findByCandidateId(candidateId)).thenReturn(Optional.of(session));

        var result = interviewService.getCandidateDetails(candidateId);

        assertThat(result.name()).isEqualTo("Jane");
        assertThat(result.email()).isEqualTo("jane@test.com");
        assertThat(result.score()).isEqualTo(8.5);
        assertThat(result.strengths()).containsExactly("React", "Node");
        assertThat(result.weaknesses()).containsExactly("CSS");
        assertThat(result.questions()).hasSize(2);
    }

    @Test
    void getCandidateDetails_candidateNotFound_throws() {
        UUID candidateId = UUID.randomUUID();
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.getCandidateDetails(candidateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Candidate not found");
    }

    @Test
    void getCandidateDetails_sessionNotFound_throws() {
        UUID candidateId = UUID.randomUUID();
        Candidate candidate = new Candidate();
        candidate.setId(candidateId);
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(interviewSessionRepository.findByCandidateId(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewService.getCandidateDetails(candidateId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Interview session not found");
    }

    @Test
    void getAllCandidates_returnsSummaries() {
        Candidate c1 = new Candidate();
        c1.setId(UUID.randomUUID());
        c1.setName("Alice");
        c1.setScore(9.0);
        c1.setSummary("Excellent");
        Candidate c2 = new Candidate();
        c2.setId(UUID.randomUUID());
        c2.setName("Bob");
        c2.setScore(6.0);
        c2.setSummary("Average");
        when(candidateRepository.findAll()).thenReturn(List.of(c1, c2));

        var results = interviewService.getAllCandidates();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).name()).isEqualTo("Alice");
        assertThat(results.get(1).name()).isEqualTo("Bob");
    }

    private byte[] createTestPdf() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private MultipartFile createMockResume() {
        return new MockMultipartFile("resume", "test.pdf", "application/pdf", testPdfBytes);
    }

    private ResumeAnalysisDTO createMockAnalysis() {
        return new ResumeAnalysisDTO(
                List.of("React", "Node.js"),
                List.of("Database design"),
                new ResumeAnalysisDTO.Recommendation("Recommended", "Strong candidate"),
                new ResumeAnalysisDTO.GraphData(
                        Map.of("React", 8, "Node.js", 7),
                        Map.of("Frontend", 60, "Backend", 40)));
    }

    private InterviewSession createSessionWithQuestions(int numQuestions, int currentIndex) {
        InterviewSession session = new InterviewSession();
        session.setId(UUID.randomUUID());
        session.setStatus(InterviewStatus.IN_PROGRESS);
        session.setCurrentQuestionIndex(currentIndex);

        Candidate candidate = new Candidate();
        candidate.setName("Test Candidate");
        session.setCandidate(candidate);

        QuestionDifficulty[] difficulties = {
                QuestionDifficulty.EASY, QuestionDifficulty.MEDIUM, QuestionDifficulty.HARD,
                QuestionDifficulty.EASY, QuestionDifficulty.MEDIUM, QuestionDifficulty.HARD
        };
        String[] texts = {
                "What is React?", "Explain hooks", "Design a system",
                "What is JSX?", "Context API", "Optimize rendering"
        };

        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < numQuestions && i < difficulties.length; i++) {
            Question q = new Question();
            q.setDifficulty(difficulties[i]);
            q.setQuestionText(texts[i]);
            q.setSession(session);
            questions.add(q);
        }
        session.setQuestions(questions);

        return session;
    }
}
