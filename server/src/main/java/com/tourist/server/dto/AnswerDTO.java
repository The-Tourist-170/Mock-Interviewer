package com.tourist.server.dto;

import jakarta.validation.constraints.NotBlank;

public record AnswerDTO(@NotBlank String answer) {}