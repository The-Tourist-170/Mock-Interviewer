package com.tourist.server.controller;

import com.tourist.server.dto.AnswerDTO;
import com.tourist.server.dto.CandidateDetailDTO;
import com.tourist.server.dto.CandidateSummaryDTO;
import com.tourist.server.dto.InterviewStateDTO;
import com.tourist.server.service.InterviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
@Validated
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/start")
    public ResponseEntity<InterviewStateDTO> startInterview(
            @RequestParam("name") @NotBlank(message = "Name is required") String name,
            @RequestParam("email") @NotBlank(message = "Email is required") String email,
            @RequestParam("phone") @NotBlank(message = "Phone is required") String phone,
            @RequestParam("resume") MultipartFile resume) throws IOException {
        InterviewStateDTO initialState = interviewService.startInterview(name, email, phone, resume);
        return new ResponseEntity<>(initialState, HttpStatus.CREATED);
    }

    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<InterviewStateDTO> submitAnswer(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AnswerDTO answerDTO) throws IOException {
        InterviewStateDTO nextState = interviewService.submitAnswer(sessionId, answerDTO);
        return ResponseEntity.ok(nextState);
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<CandidateSummaryDTO>> getAllCandidates() {
        List<CandidateSummaryDTO> candidates = interviewService.getAllCandidates();
        return ResponseEntity.ok(candidates);
    }

    @GetMapping("/candidates/{candidateId}")
    public ResponseEntity<CandidateDetailDTO> getCandidateDetails(@PathVariable UUID candidateId) {
        CandidateDetailDTO candidateDetails = interviewService.getCandidateDetails(candidateId);
        return ResponseEntity.ok(candidateDetails);
    }
}