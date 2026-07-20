package com.tourist.server.dto;

import com.tourist.server.model.QuestionDifficulty;

public record QuestionDTO(
        String questionText,
        QuestionDifficulty difficulty,
        String candidateAnswer,
        Integer aiScore,
        String aiFeedback
) {}