import React from 'react'
import { screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { renderWithProviders } from '../test/test-utils.jsx'
import Home from './Home'

describe('Home', () => {
  it('renders both tabs with correct labels', () => {
    renderWithProviders(<Home />)
    expect(screen.getByText('Interviewer')).toBeInTheDocument()
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
  })

  it('shows NewInterview (upload UI) by default', () => {
    renderWithProviders(<Home />)
    expect(screen.getByText('Start New Interview')).toBeInTheDocument()
  })

  it('switches to Dashboard view when Dashboard tab is clicked', async () => {
    renderWithProviders(<Home />)
    fireEvent.click(screen.getByText('Dashboard'))
    await waitFor(() => {
      expect(screen.getByText('Test Candidate')).toBeInTheDocument()
    })
    expect(screen.queryByText('Start New Interview')).not.toBeInTheDocument()
  })

  it('switches back to Interviewer view when Interviewer tab is clicked', async () => {
    renderWithProviders(<Home />)
    fireEvent.click(screen.getByText('Dashboard'))
    await waitFor(() => expect(screen.getByText('Test Candidate')).toBeInTheDocument())
    fireEvent.click(screen.getByText('Interviewer'))
    await waitFor(() => {
      expect(screen.getByText('Start New Interview')).toBeInTheDocument()
    })
  })
})
