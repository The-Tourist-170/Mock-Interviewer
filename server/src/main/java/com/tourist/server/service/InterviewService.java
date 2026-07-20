package com.tourist.server.service;

import com.tourist.server.dto.*;
import com.tourist.server.exception.ResourceNotFoundException;
import com.tourist.server.model.*;
import com.tourist.server.repository.CandidateRepository;
import com.tourist.server.repository.InterviewSessionRepository;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class InterviewService {

    private final CandidateRepository candidateRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final FileStorageService fileStorageService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public InterviewService(CandidateRepository candidateRepository,
            InterviewSessionRepository interviewSessionRepository,
            FileStorageService fileStorageService,
            AiService aiService,
            ObjectMapper objectMapper) {
        this.candidateRepository = candidateRepository;
        this.interviewSessionRepository = interviewSessionRepository;
        this.fileStorageService = fileStorageService;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InterviewStateDTO startInterview(String name, String email, String phone, MultipartFile resume)
            throws IOException {
        String resumePath = fileStorageService.storeFile(resume);

        Candidate candidate = new Candidate();
        candidate.setName(name);
        candidate.setEmail(email);
        candidate.setPhone(phone);
        candidate.setResumePath(resumePath);
        candidateRepository.save(candidate);

        ResumeAnalysisDTO resumeAnalysis = analyzeAndSetCandidateDetails(candidate, resume);

        List<Question> previousQuestions = interviewSessionRepository.findFirstByOrderByCreationTimestampDesc()
                .map(InterviewSession::getQuestions)
                .orElse(Collections.emptyList());

        InterviewSession session = new InterviewSession();
        session.setCandidate(candidate);
        session.setStatus(InterviewStatus.IN_PROGRESS);

        String rawQuestions = aiService.generateQuestions(previousQuestions, resumeAnalysis);
        List<Question> questionList = parseQuestions(rawQuestions, session);
        if (questionList.isEmpty()) {
            throw new RuntimeException("Failed to generate interview questions from AI response. Please try again.");
        }
        session.setQuestions(questionList);

        interviewSessionRepository.save(session);
        return mapToInterviewStateDTO(session);
    }

    @Transactional
    public InterviewStateDTO submitAnswer(UUID sessionId, AnswerDTO answerDTO) throws IOException {
        InterviewSession session = interviewSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session not found with id: " + sessionId));

        int questionIndex = session.getCurrentQuestionIndex();
        Question currentQuestion = session.getQuestions().get(questionIndex);
        currentQuestion.setCandidateAnswer(answerDTO.answer());

        String evaluation = aiService.evaluateAnswer(currentQuestion.getQuestionText(), answerDTO.answer());
        parseEvaluation(evaluation, currentQuestion);

        if (questionIndex + 1 >= session.getQuestions().size()) {
            session.setStatus(InterviewStatus.COMPLETED);
            calculateFinalScoreAndSummary(session);
        } else {
            session.setCurrentQuestionIndex(questionIndex + 1);
        }

        interviewSessionRepository.save(session);
        return mapToInterviewStateDTO(session);
    }

    public List<CandidateSummaryDTO> getAllCandidates() {
        return candidateRepository.findAll().stream()
                .map(c -> new CandidateSummaryDTO(c.getId(), c.getName(), c.getScore(), c.getSummary()))
                .collect(Collectors.toList());
    }

    public CandidateDetailDTO getCandidateDetails(UUID candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));

        InterviewSession session = interviewSessionRepository.findByCandidateId(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interview session not found for candidate: " + candidateId));

        List<QuestionDTO> questionDTOs = session.getQuestions().stream()
                .map(q -> new QuestionDTO(q.getQuestionText(), q.getDifficulty(), q.getCandidateAnswer(),
                        q.getAiScore(), q.getAiFeedback()))
                .collect(Collectors.toList());

        return new CandidateDetailDTO(
                candidate.getId(), candidate.getName(), candidate.getEmail(), candidate.getPhone(),
                candidate.getScore(), candidate.getSummary(), questionDTOs,
                candidate.getStrengths(), candidate.getWeaknesses(),
                candidate.getRecommendationVerdict(), candidate.getRecommendationRationale(),
                candidate.getSkillRatings(), candidate.getExperienceBreakdown());
    }

    private void calculateFinalScoreAndSummary(InterviewSession session) throws IOException {
        double totalScore = session.getQuestions().stream()
                .mapToInt(q -> q.getAiScore() != null ? q.getAiScore() : 0)
                .average().orElse(0.0);
        session.getCandidate().setScore(totalScore);

        String summary = aiService.summarizePerformance(session.getQuestions());
        session.getCandidate().setSummary(summary);
    }

    private ResumeAnalysisDTO analyzeAndSetCandidateDetails(Candidate candidate, MultipartFile resumeFile) {
        try {
            String resumeText;
            try (PDDocument document = Loader.loadPDF(resumeFile.getBytes())) {
                resumeText = new PDFTextStripper().getText(document);
            }

            String rawJsonAnalysis = aiService.analyzeResume(resumeText);
            String cleanedJson = rawJsonAnalysis.replace("```json", "").replace("```", "").trim();

            ResumeAnalysisDTO analysis = objectMapper.readValue(cleanedJson, ResumeAnalysisDTO.class);

            candidate.setStrengths(analysis.strengths());
            candidate.setWeaknesses(analysis.weaknesses());
            candidate.setRecommendationVerdict(analysis.recommendation().verdict());
            candidate.setRecommendationRationale(analysis.recommendation().rationale());
            candidate.setSkillRatings(analysis.graphData().skillRatings());
            candidate.setExperienceBreakdown(analysis.graphData().experienceBreakdown());

            return analysis;

        } catch (Exception e) {
            System.err.println("Failed to analyze resume: " + e.getMessage());
            candidate.setStrengths(List.of("Analysis Failed"));
            candidate.setWeaknesses(List.of("Analysis Failed"));
            candidate.setRecommendationVerdict("Error");
            candidate.setRecommendationRationale(
                    "Could not analyze resume. Please review manually.");
            candidate.setSkillRatings(Map.of("Error", 0));
            candidate.setExperienceBreakdown(Map.of("Error", 100));
            return null;
        }
    }

    private List<Question> parseQuestions(String rawText, InterviewSession session) {
        List<Question> questions = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(EASY|MEDIUM|HARD):\\s*(.*)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(rawText);
        while (matcher.find()) {
            Question question = new Question();
            question.setDifficulty(QuestionDifficulty.valueOf(matcher.group(1)));
            question.setQuestionText(matcher.group(2).trim());
            question.setSession(session);
            questions.add(question);
        }
        return questions;
    }

    private void parseEvaluation(String rawText, Question question) {
        Pattern scorePattern = Pattern.compile("SCORE:\\s*(\\d+)");
        Matcher scoreMatcher = scorePattern.matcher(rawText);
        boolean scoreFound = scoreMatcher.find();

        Pattern feedbackPattern = Pattern.compile("FEEDBACK:\\s*(.*)");
        Matcher feedbackMatcher = feedbackPattern.matcher(rawText);
        boolean feedbackFound = feedbackMatcher.find();

        if (scoreFound && feedbackFound) {
            question.setAiScore(Integer.parseInt(scoreMatcher.group(1)));
            question.setAiFeedback(feedbackMatcher.group(1).trim());
        } else {
            question.setAiScore(0);
            question.setAiFeedback("AI evaluation failed due to an unexpected response format.");
        }
    }

    private InterviewStateDTO mapToInterviewStateDTO(InterviewSession session) {
        if (session.getStatus() == InterviewStatus.COMPLETED) {
            return new InterviewStateDTO(session.getId(), session.getCandidate().getName(), session.getStatus(),
                    session.getCurrentQuestionIndex(), session.getQuestions().size(),
                    "Interview Complete!", null, 0);
        }

        Question currentQuestion = session.getQuestions().get(session.getCurrentQuestionIndex());
        long timer = switch (currentQuestion.getDifficulty()) {
            case EASY -> 20;
            case MEDIUM -> 60;
            case HARD -> 120;
        };

        return new InterviewStateDTO(
                session.getId(),
                session.getCandidate().getName(),
                session.getStatus(),
                session.getCurrentQuestionIndex(),
                session.getQuestions().size(),
                currentQuestion.getQuestionText(),
                currentQuestion.getDifficulty(),
                timer);
    }
}