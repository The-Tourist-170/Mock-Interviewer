import React from 'react'
import { screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { renderWithProviders } from '../test/test-utils.jsx'
import NewInterview from './NewInterview'

describe('NewInterview', () => {
  it('renders upload UI on step 1', () => {
    renderWithProviders(<NewInterview />, { route: '/interview/new' })
    expect(screen.getByText('Start New Interview')).toBeInTheDocument()
    expect(screen.getByText('Click to Upload Resume')).toBeInTheDocument()
  })

  it('accepts a PDF and moves to step 2 with pre-filled form', async () => {
    renderWithProviders(<NewInterview />, { route: '/interview/new' })
    const input = document.querySelector('input[type="file"]')
    const file = new File(['fake pdf'], 'resume.pdf', { type: 'application/pdf' })
    fireEvent.change(input, { target: { files: [file] } })
    await waitFor(() => {
      expect(screen.getByText('Confirm Candidate Details')).toBeInTheDocument()
    })
    expect(screen.getByDisplayValue('John Doe')).toBeInTheDocument()
    expect(screen.getByDisplayValue('john@example.com')).toBeInTheDocument()
  })

  it('rejects non-PDF files and stays on step 1', async () => {
    renderWithProviders(<NewInterview />, { route: '/interview/new' })
    const input = document.querySelector('input[type="file"]')
    const file = new File(['text'], 'resume.txt', { type: 'text/plain' })
    fireEvent.change(input, { target: { files: [file] } })
    expect(screen.getByText('Click to Upload Resume')).toBeInTheDocument()
    expect(screen.queryByText('Confirm Candidate Details')).not.toBeInTheDocument()
  })

  it('dispatches interviewStarted on valid submit', async () => {
    const { store } = renderWithProviders(<NewInterview />, {
      route: '/interview/new',
    })
    const dispatchSpy = vi.spyOn(store, 'dispatch')
    const input = document.querySelector('input[type="file"]')
    const file = new File(['fake pdf'], 'resume.pdf', { type: 'application/pdf' })
    fireEvent.change(input, { target: { files: [file] } })
    await waitFor(() =>
      expect(screen.getByText('Confirm Candidate Details')).toBeInTheDocument()
    )
    fireEvent.click(screen.getByRole('button', { name: /Start Interview/i }))
    await waitFor(() => {
      expect(dispatchSpy).toHaveBeenCalledWith(
        expect.objectContaining({ type: 'interview/interviewStarted' })
      )
    })
  })

  it('does not dispatch interviewStarted when fields are missing', async () => {
    const { store } = renderWithProviders(<NewInterview />, {
      route: '/interview/new',
    })
    const dispatchSpy = vi.spyOn(store, 'dispatch')
    const input = document.querySelector('input[type="file"]')
    const file = new File(['fake pdf'], 'resume.pdf', { type: 'application/pdf' })
    fireEvent.change(input, { target: { files: [file] } })
    await waitFor(() =>
      expect(screen.getByText('Confirm Candidate Details')).toBeInTheDocument()
    )
    fireEvent.change(screen.getByPlaceholderText('Full Name'), {
      target: { value: '' },
    })
    fireEvent.click(screen.getByRole('button', { name: /Start Interview/i }))
    expect(dispatchSpy).not.toHaveBeenCalledWith(
      expect.objectContaining({ type: 'interview/interviewStarted' })
    )
  })
})
