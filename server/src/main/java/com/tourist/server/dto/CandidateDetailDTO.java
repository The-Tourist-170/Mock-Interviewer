package com.tourist.server.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CandidateDetailDTO(
        UUID id,
        String name,
        String email,
        String phone,
        Double score,
        String summary,
        List<QuestionDTO> questions,
        List<String> strengths,
        List<String> weaknesses,
        String recommendationVerdict,
        String recommendationRationale,
        Map<String, Integer> skillRatings,
        Map<String, Integer> experienceBreakdown
) {}