package com.tourist.server.dto;

import java.util.List;

public record GeminiResponse(List<Candidate> candidates) {
    public String text() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate firstCandidate = candidates.get(0);
            if (firstCandidate.content() != null && !firstCandidate.content().parts().isEmpty()) {
                return firstCandidate.content().parts().get(0).text();
            }
        }
        return "";
    }
    
    public static record Candidate(Content content) {}
    public static record Content(List<Part> parts) {}
    public static record Part(String text) {}
}