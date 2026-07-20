import { describe, it, expect } from 'vitest'
import { server } from '../test/server.js'
import { http, HttpResponse } from 'msw'
import {
  extractResumeInfo,
  startInterviewAPI,
  submitAnswerAPI,
  fetchCandidatesAPI,
  fetchCandidateDetailAPI,
} from './apiService'
import {
  mockInterviewState,
  mockExtractedInfo,
  mockCandidateSummary,
  mockCandidateDetail,
} from '../test/handlers.js'

const API_BASE = 'http://localhost:8080/api'

describe('apiService', () => {
  describe('extractResumeInfo', () => {
    it('returns parsed JSON on success', async () => {
      const file = new File(['pdf'], 'resume.pdf', { type: 'application/pdf' })
      const data = await extractResumeInfo(file)
      expect(data).toEqual(mockExtractedInfo)
    })

    it('throws on non-2xx response', async () => {
      server.use(
        http.post(`${API_BASE}/resumes/extract-info`, () =>
          HttpResponse.json({ error: 'bad' }, { status: 500 })
        )
      )
      const file = new File(['pdf'], 'resume.pdf', { type: 'application/pdf' })
      await expect(extractResumeInfo(file)).rejects.toThrow(
        'Failed to extract resume information.'
      )
    })
  })

  describe('startInterviewAPI', () => {
    it('returns parsed JSON on success', async () => {
      const data = await startInterviewAPI({
        name: 'John',
        email: 'john@example.com',
        phone: '555-0100',
        resume: new File(['pdf'], 'resume.pdf', { type: 'application/pdf' }),
      })
      expect(data).toEqual(mockInterviewState)
    })

    it('throws on non-2xx response', async () => {
      server.use(
        http.post(`${API_BASE}/interviews/start`, () =>
          HttpResponse.json({ error: 'bad' }, { status: 500 })
        )
      )
      await expect(
        startInterviewAPI({
          name: 'John',
          email: 'john@example.com',
          phone: '555-0100',
          resume: new File(['pdf'], 'resume.pdf', { type: 'application/pdf' }),
        })
      ).rejects.toThrow('Failed to start the interview.')
    })
  })

  describe('submitAnswerAPI', () => {
    it('returns parsed JSON on success', async () => {
      const data = await submitAnswerAPI('session-1', 'my answer')
      expect(data.currentQuestionIndex).toBe(1)
      expect(data.currentQuestionText).toBe('Describe a challenging project.')
    })

    it('sends JSON body with answer', async () => {
      let capturedBody = null
      server.use(
        http.post(`${API_BASE}/interviews/:sessionId/answer`, async ({ request }) => {
          capturedBody = await request.json()
          return HttpResponse.json(mockInterviewState)
        })
      )
      await submitAnswerAPI('session-1', 'my answer')
      expect(capturedBody).toEqual({ answer: 'my answer' })
    })

    it('throws on non-2xx response', async () => {
      server.use(
        http.post(`${API_BASE}/interviews/:sessionId/answer`, () =>
          HttpResponse.json({ error: 'bad' }, { status: 500 })
        )
      )
      await expect(submitAnswerAPI('session-1', 'my answer')).rejects.toThrow(
        'Failed to submit answer.'
      )
    })
  })

  describe('fetchCandidatesAPI', () => {
    it('returns array on success', async () => {
      const data = await fetchCandidatesAPI()
      expect(data).toEqual([mockCandidateSummary])
    })

    it('throws on non-2xx response', async () => {
      server.use(
        http.get(`${API_BASE}/interviews/candidates`, () =>
          HttpResponse.json({ error: 'bad' }, { status: 500 })
        )
      )
      await expect(fetchCandidatesAPI()).rejects.toThrow(
        'Failed to fetch candidates.'
      )
    })
  })

  describe('fetchCandidateDetailAPI', () => {
    it('returns parsed JSON on success', async () => {
      const data = await fetchCandidateDetailAPI('test-candidate-id')
      expect(data.id).toBe('test-candidate-id')
      expect(data.strengths).toEqual(mockCandidateDetail.strengths)
    })

    it('throws on 404', async () => {
      await expect(fetchCandidateDetailAPI('not-found')).rejects.toThrow(
        'Failed to fetch candidate details.'
      )
    })
  })
})
