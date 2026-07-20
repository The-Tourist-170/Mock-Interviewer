package com.tourist.server.dto;

import java.util.UUID;

import com.tourist.server.model.InterviewStatus;
import com.tourist.server.model.QuestionDifficulty;

public record InterviewStateDTO(
        UUID sessionId,
        String candidateName,
        InterviewStatus status,
        int currentQuestionIndex,
        int totalQuestions,
        String currentQuestionText,
        QuestionDifficulty difficulty,
        long timer
) {}