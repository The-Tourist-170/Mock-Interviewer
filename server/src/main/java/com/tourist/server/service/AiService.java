package com.tourist.server.service;

import com.tourist.server.dto.ResumeAnalysisDTO;
import com.tourist.server.model.Question;
import java.util.List;

public interface AiService {
    String generateQuestions(List<Question> previousQuestions, ResumeAnalysisDTO resumeAnalysis);
    String extractInfoFromResume(String resumeText);
    String evaluateAnswer(String question, String answer);
    String analyzeResume(String resumeText);
    String summarizePerformance(List<Question> questions);
}
