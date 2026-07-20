import React from 'react'
import { screen, waitFor } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { server } from '../test/server.js'
import { http, HttpResponse } from 'msw'
import { renderWithProviders } from '../test/test-utils.jsx'
import Dashboard from './Dashboard'

const API_BASE = 'http://localhost:8080/api'

describe('Dashboard', () => {
  it('renders candidate rows after successful fetch', async () => {
    renderWithProviders(<Dashboard />)
    await waitFor(() => {
      expect(screen.getByText('Test Candidate')).toBeInTheDocument()
    })
    expect(screen.getByText('8 / 10')).toBeInTheDocument()
  })

  it('renders error message when fetch fails', async () => {
    server.use(
      http.get(`${API_BASE}/interviews/candidates`, () =>
        HttpResponse.json({ error: 'bad' }, { status: 500 })
      )
    )
    renderWithProviders(<Dashboard />)
    await waitFor(() => {
      expect(screen.getByText('Failed to fetch candidates.')).toBeInTheDocument()
    })
  })

  it('renders empty-state message when no candidates exist', async () => {
    server.use(
      http.get(`${API_BASE}/interviews/candidates`, () =>
        HttpResponse.json([])
      )
    )
    renderWithProviders(<Dashboard />)
    await waitFor(() => {
      expect(
        screen.getByText('No candidates found. Start a new interview!')
      ).toBeInTheDocument()
    })
  })
})
