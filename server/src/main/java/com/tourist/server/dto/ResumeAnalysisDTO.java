package com.tourist.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record ResumeAnalysisDTO(
    List<String> strengths,
    List<String> weaknesses,
    Recommendation recommendation,
    @JsonProperty("graph_data") GraphData graphData
) {
    public record Recommendation(
        String verdict,
        String rationale
    ) {}

    public record GraphData(
        @JsonProperty("skill_ratings") Map<String, Integer> skillRatings, 
        @JsonProperty("experience_breakdown") Map<String, Integer> experienceBreakdown 
    ) {}
}