package com.tourist.server.dto;

import java.util.UUID;

public record CandidateSummaryDTO(UUID id, String name, Double score, String summary) {}