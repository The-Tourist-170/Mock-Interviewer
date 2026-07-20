package com.tourist.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourist.server.dto.AnswerDTO;
import com.tourist.server.dto.CandidateDetailDTO;
import com.tourist.server.dto.CandidateSummaryDTO;
import com.tourist.server.dto.InterviewStateDTO;
import com.tourist.server.exception.GlobalExceptionHandler;
import com.tourist.server.exception.ResourceNotFoundException;
import com.tourist.server.model.InterviewStatus;
import com.tourist.server.model.QuestionDifficulty;
import com.tourist.server.service.InterviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterviewController.class)
@Import(GlobalExceptionHandler.class)
class InterviewControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InterviewService interviewService;

    @Test
    void startInterview_missingName_returns400() throws Exception {
        MockMultipartFile resume = new MockMultipartFile("resume", "resume.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/api/interviews/start")
                        .file(resume)
                        .param("email", "test@example.com")
                        .param("phone", "555-1234"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInterview_missingEmail_returns400() throws Exception {
        MockMultipartFile resume = new MockMultipartFile("resume", "resume.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/api/interviews/start")
                        .file(resume)
                        .param("name", "Test User")
                        .param("phone", "555-1234"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInterview_missingPhone_returns400() throws Exception {
        MockMultipartFile resume = new MockMultipartFile("resume", "resume.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/api/interviews/start")
                        .file(resume)
                        .param("name", "Test User")
                        .param("email", "test@example.com"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startInterview_validRequest_returns201() throws Exception {
        MockMultipartFile resume = new MockMultipartFile("resume", "resume.pdf", "application/pdf", new byte[]{1});
        UUID sessionId = UUID.randomUUID();
        InterviewStateDTO state = new InterviewStateDTO(
                sessionId, "Test User", InterviewStatus.IN_PROGRESS, 0, 6,
                "Tell me about your React experience.", QuestionDifficulty.EASY, 20);

        when(interviewService.startInterview(eq("Test User"), eq("test@example.com"), eq("555-1234"), any()))
                .thenReturn(state);

        mockMvc.perform(multipart("/api/interviews/start")
                        .file(resume)
                        .param("name", "Test User")
                        .param("email", "test@example.com")
                        .param("phone", "555-1234"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.candidateName").value("Test User"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentQuestionIndex").value(0))
                .andExpect(jsonPath("$.totalQuestions").value(6))
                .andExpect(jsonPath("$.currentQuestionText").value("Tell me about your React experience."))
                .andExpect(jsonPath("$.difficulty").value("EASY"))
                .andExpect(jsonPath("$.timer").value(20));
    }

    @Test
    void submitAnswer_blankAnswer_returns400() throws Exception {
        UUID sessionId = UUID.randomUUID();
        AnswerDTO blank = new AnswerDTO("");

        mockMvc.perform(post("/api/interviews/{sessionId}/answer", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blank)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitAnswer_validAnswer_returns200() throws Exception {
        UUID sessionId = UUID.randomUUID();
        AnswerDTO answer = new AnswerDTO("My answer to the question.");
        InterviewStateDTO state = new InterviewStateDTO(
                sessionId, "Test User", InterviewStatus.IN_PROGRESS, 1, 6,
                "Explain how you would design a database schema.", QuestionDifficulty.MEDIUM, 60);

        when(interviewService.submitAnswer(eq(sessionId), any(AnswerDTO.class))).thenReturn(state);

        mockMvc.perform(post("/api/interviews/{sessionId}/answer", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestionIndex").value(1))
                .andExpect(jsonPath("$.difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.timer").value(60));
    }

    @Test
    void getCandidateDetails_notFound_returns404() throws Exception {
        UUID candidateId = UUID.randomUUID();
        when(interviewService.getCandidateDetails(candidateId))
                .thenThrow(new ResourceNotFoundException("Candidate not found with id: " + candidateId));

        mockMvc.perform(get("/api/interviews/candidates/{candidateId}", candidateId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Candidate not found with id: " + candidateId));
    }

    @Test
    void getCandidateDetails_found_returns200() throws Exception {
        UUID candidateId = UUID.randomUUID();
        CandidateDetailDTO detail = new CandidateDetailDTO(
                candidateId, "Test User", "test@example.com", "555-1234",
                8.0, "Strong candidate.",
                List.of(),
                List.of("React", "Node.js"), List.of("Database design"),
                "Recommended", "Good fit for the role.",
                Map.of("React", 9, "Node.js", 8), Map.of("Frontend", 60, "Backend", 40));

        when(interviewService.getCandidateDetails(candidateId)).thenReturn(detail);

        mockMvc.perform(get("/api/interviews/candidates/{candidateId}", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateId.toString()))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.score").value(8.0))
                .andExpect(jsonPath("$.strengths[0]").value("React"))
                .andExpect(jsonPath("$.weaknesses[0]").value("Database design"))
                .andExpect(jsonPath("$.recommendationVerdict").value("Recommended"));
    }

    @Test
    void getAllCandidates_returns200WithList() throws Exception {
        UUID id1 = UUID.randomUUID();
        CandidateSummaryDTO c1 = new CandidateSummaryDTO(id1, "Alice", 8.5, "Great.");
        CandidateSummaryDTO c2 = new CandidateSummaryDTO(UUID.randomUUID(), "Bob", null, null);

        when(interviewService.getAllCandidates()).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/interviews/candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(id1.toString()))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].score").value(8.5))
                .andExpect(jsonPath("$[1].name").value("Bob"))
                .andExpect(jsonPath("$[1].score").doesNotExist());
    }

    @Test
    void submitAnswer_sessionNotFound_returns404() throws Exception {
        UUID sessionId = UUID.randomUUID();
        AnswerDTO answer = new AnswerDTO("Some answer.");
        when(interviewService.submitAnswer(eq(sessionId), any(AnswerDTO.class)))
                .thenThrow(new ResourceNotFoundException("Interview session not found with id: " + sessionId));

        mockMvc.perform(post("/api/interviews/{sessionId}/answer", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Interview session not found with id: " + sessionId));
    }
}
