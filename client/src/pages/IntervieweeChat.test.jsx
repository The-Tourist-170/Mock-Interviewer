import React from 'react'
import { screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { renderWithProviders } from '../test/test-utils.jsx'
import IntervieweeChat from './IntervieweeChat'

const inProgressState = {
  interview: {
    sessionId: 'test-session-id',
    questions: ['Tell me about your experience with React.'],
    answers: [],
    status: 'IN_PROGRESS',
    difficulty: 'EASY',
    timer: 20,
    isLoading: false,
    error: null,
    resumeFile: null,
  },
}

const completedState = {
  interview: {
    sessionId: 'test-session-id',
    questions: ['Tell me about your experience with React.'],
    answers: ['I have 5 years of experience.'],
    status: 'COMPLETED',
    difficulty: 'EASY',
    timer: 0,
    isLoading: false,
    error: null,
    resumeFile: null,
  },
}

describe('IntervieweeChat', () => {
  it('renders existing question messages', () => {
    renderWithProviders(<IntervieweeChat />, {
      route: '/interview/active/test-session-id',
      preloadedState: inProgressState,
    })
    expect(
      screen.getByText('Tell me about your experience with React.')
    ).toBeInTheDocument()
  })

  it('renders the Timer with the current duration', () => {
    renderWithProviders(<IntervieweeChat />, {
      route: '/interview/active/test-session-id',
      preloadedState: inProgressState,
    })
    expect(screen.getByText('00:20')).toBeInTheDocument()
  })

  it('submits answer and receives next question', async () => {
    const { store } = renderWithProviders(<IntervieweeChat />, {
      route: '/interview/active/test-session-id',
      preloadedState: inProgressState,
    })
    const input = screen.getByPlaceholderText('Type your answer...')
    fireEvent.change(input, { target: { value: 'I have 5 years of experience.' } })
    fireEvent.click(screen.getByRole('button'))
    await waitFor(() => {
      expect(
        screen.getByText('Describe a challenging project.')
      ).toBeInTheDocument()
    })
    expect(store.getState().interview.answers).toHaveLength(1)
    expect(store.getState().interview.questions).toHaveLength(2)
  })

  it('renders Home button when status is COMPLETED', () => {
    renderWithProviders(<IntervieweeChat />, {
      route: '/interview/active/test-session-id',
      preloadedState: completedState,
    })
    expect(screen.getByText('Home')).toBeInTheDocument()
  })

  it('dispatches resetInterview when Home button is clicked', () => {
    const { store } = renderWithProviders(<IntervieweeChat />, {
      route: '/interview/active/test-session-id',
      preloadedState: completedState,
    })
    fireEvent.click(screen.getByText('Home'))
    expect(store.getState().interview.status).toBe('IDLE')
    expect(store.getState().interview.questions).toHaveLength(0)
  })
})
