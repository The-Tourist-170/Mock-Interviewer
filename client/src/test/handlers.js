import { http, HttpResponse } from 'msw'

const API_BASE = 'http://localhost:8080/api'

export const mockInterviewState = {
  sessionId: 'test-session-id',
  candidateName: 'Test Candidate',
  status: 'IN_PROGRESS',
  currentQuestionIndex: 0,
  totalQuestions: 6,
  currentQuestionText: 'Tell me about your experience with React.',
  difficulty: 'EASY',
  timer: 20,
}

export const mockCandidateSummary = {
  id: 'test-candidate-id',
  name: 'Test Candidate',
  score: 8.0,
  summary: 'Strong candidate with good technical skills.',
}

export const mockCandidateDetail = {
  id: 'test-candidate-id',
  name: 'Test Candidate',
  email: 'test@example.com',
  phone: '555-0100',
  score: 8.0,
  summary: 'Strong candidate with good technical skills.',
  questions: [
    {
      questionText: 'Tell me about your experience with React.',
      difficulty: 'EASY',
      candidateAnswer: 'I have 5 years of experience...',
      aiScore: 8,
      aiFeedback: 'Good answer.',
    },
  ],
  strengths: ['React', 'Node.js'],
  weaknesses: ['Database design'],
  recommendationVerdict: 'RECOMMENDED',
  recommendationRationale: 'Strong technical background.',
  skillRatings: { React: 8, NodeJS: 7, Database: 5 },
  experienceBreakdown: { Frontend: 60, Backend: 30, DevOps: 10 },
}

export const mockExtractedInfo = {
  name: 'John Doe',
  email: 'john@example.com',
  phone: '555-0100',
}

export const handlers = [
  http.post(`${API_BASE}/resumes/extract-info`, () =>
    HttpResponse.json(mockExtractedInfo)
  ),
  http.post(`${API_BASE}/interviews/start`, () =>
    HttpResponse.json(mockInterviewState, { status: 201 })
  ),
  http.post(`${API_BASE}/interviews/:sessionId/answer`, () =>
    HttpResponse.json({
      ...mockInterviewState,
      currentQuestionIndex: 1,
      currentQuestionText: 'Describe a challenging project.',
      difficulty: 'MEDIUM',
      timer: 60,
    })
  ),
  http.get(`${API_BASE}/interviews/candidates`, () =>
    HttpResponse.json([mockCandidateSummary])
  ),
  http.get(`${API_BASE}/interviews/candidates/:candidateId`, ({ params }) => {
    if (params.candidateId === 'not-found') {
      return new HttpResponse(null, { status: 404 })
    }
    return HttpResponse.json({
      ...mockCandidateDetail,
      id: params.candidateId,
    })
  }),
]
