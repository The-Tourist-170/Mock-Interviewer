package com.tourist.server.service;

import com.tourist.server.dto.ResumeAnalysisDTO;
import com.tourist.server.model.Question;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

final class AiPrompts {

    private AiPrompts() {}

    private static final List<String> QUESTION_TOPICS = List.of(
            "asynchronous programming and Promises",
            "React hooks and state management",
            "API security and authentication",
            "CSS layout techniques like Flexbox or Grid",
            "database indexing and optimization",
            "Node.js event loop");

    static String generateQuestionsPrompt(List<Question> previousQuestions,
            ResumeAnalysisDTO resumeAnalysis) {
        String randomTopic = QUESTION_TOPICS.get(new Random().nextInt(QUESTION_TOPICS.size()));

        String previousQuestionsContext = "";
        if (previousQuestions != null && !previousQuestions.isEmpty()) {
            String formattedOldQuestions = previousQuestions.stream()
                    .map(Question::getQuestionText)
                    .collect(Collectors.joining("\n- ", "- ", ""));
            previousQuestionsContext = String.format("""

                    For context, here are the questions from the previous interview session.
                    Strictly avoid repeating any of these questions or their close variations:
                    %s
                    """, formattedOldQuestions);
        }

        String resumeContext = "";
        if (resumeAnalysis != null) {
            resumeContext = buildResumeContext(resumeAnalysis);
        }

        return String.format("""
                        Generate a unique set of 6 interview questions for a full-stack developer role (React/Node.js).
                        For this set, please include at least one question related to %s.
                        Provide 2 easy, 2 medium, and 2 hard questions.
                        Format the output strictly as follows, with each question on a new line:
                        EASY: [Question Text]
                        EASY: [Question Text]
                        MEDIUM: [Question Text]
                        MEDIUM: [Question Text]
                        HARD: [Question Text]
                        HARD: [Question Text]
                        %s
                        %s
                        """,
                randomTopic, previousQuestionsContext, resumeContext);
    }

    private static String buildResumeContext(ResumeAnalysisDTO analysis) {
        String strengths = analysis.strengths() != null
                ? String.join(", ", analysis.strengths()) : "N/A";
        String weaknesses = analysis.weaknesses() != null
                ? String.join(", ", analysis.weaknesses()) : "N/A";
        String skillRatings = formatMap(analysis.graphData() != null
                ? analysis.graphData().skillRatings() : null);
        String experience = formatMap(analysis.graphData() != null
                ? analysis.graphData().experienceBreakdown() : null);

        return String.format("""

                Candidate resume analysis:
                - Strengths: %s
                - Weaknesses: %s
                - Skill ratings: %s
                - Experience: %s
                Generate questions that probe the weaknesses and test the claimed strengths.
                """, strengths, weaknesses, skillRatings, experience);
    }

    private static String formatMap(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "N/A";
        }
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    static String extractInfoFromResumePrompt(String resumeText) {
        return String.format("""
                        From the following resume text, extract the candidate's full name, email address, and phone number.
                        Return the data as a single, clean JSON object with the keys "name", "email", and "phone".
                        If a specific field cannot be found, its value in the JSON should be null. Do not add any other text or formatting.

                        Resume Text:
                        ---
                        %s
                        ---
                        """,
                resumeText);
    }

    static String evaluateAnswerPrompt(String question, String answer) {
        return String.format(
                "Evaluate the following answer for the given interview question. "
                        + "Provide a score from 1 to 10 and brief feedback (under 20 words). "
                        + "Give 0 if there is no answer, or user enters Sorry I do not know, or I have no idea or something like this not relevant to answer.\n\n"
                        + "Format the output strictly as: `SCORE: [score]\nFEEDBACK: [feedback]`.\n\n"
                        + "Question: \"%s\"\nAnswer: \"%s\"",
                question, answer);
    }

    static String analyzeResumePrompt(String resumeText) {
        return String.format("""
                        Analyze the following resume text for a full-stack developer role.
                        Based on the text, generate a JSON object with the following strict schema:
                        {
                          "strengths": ["list of 3-4 key strengths"],
                          "weaknesses": ["list of 2-3 potential weaknesses or areas for improvement"],
                          "recommendation": {
                            "verdict": "Provide a clear verdict: 'Recommended', 'Consider with reservations', or 'Not recommended'",
                            "rationale": "A brief, one-sentence rationale for the verdict."
                          },
                          "graph_data": {
                            "skill_ratings": {
                              "List up to 5 key technical skills found in the resume and rate them on a scale of 1 to 10": 0
                            },
                            "experience_breakdown": {
                              "Estimate the percentage breakdown of experience between 'Frontend', 'Backend', and 'DevOps'. The sum must be 100": 0
                            }
                          }
                        }

                        Resume Text:
                        ---
                        %s
                        ---
                        """,
                resumeText);
    }

    static String summarizePerformancePrompt(List<Question> questions) {
        StringBuilder performance = new StringBuilder();
        for (Question q : questions) {
            performance.append(String.format("Q: %s\nA: %s\nScore: %d\nFeedback: %s\n\n",
                    q.getQuestionText(), q.getCandidateAnswer(), q.getAiScore(),
                    q.getAiFeedback()));
        }
        return String.format(
                "Based on the following interview transcript, provide a concise summary (under 50 words) "
                        + "of the candidate's performance, highlighting strengths and weaknesses.\n\n"
                        + "Transcript:\n%s",
                performance);
    }
}
