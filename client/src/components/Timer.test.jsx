import React from 'react'
import { render, screen, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import Timer from './Timer'

describe('Timer', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders MM:SS from duration', () => {
    render(<Timer duration={20} onTimeUp={() => {}} difficulty="EASY" />)
    expect(screen.getByText('00:20')).toBeInTheDocument()
  })

  it('renders hours-free format for durations over 60 seconds', () => {
    render(<Timer duration={90} onTimeUp={() => {}} difficulty="MEDIUM" />)
    expect(screen.getByText('01:30')).toBeInTheDocument()
  })

  it('counts down each second', () => {
    render(<Timer duration={20} onTimeUp={() => {}} difficulty="EASY" />)
    act(() => vi.advanceTimersByTime(1000))
    expect(screen.getByText('00:19')).toBeInTheDocument()
    act(() => vi.advanceTimersByTime(9000))
    expect(screen.getByText('00:10')).toBeInTheDocument()
  })

  it('calls onTimeUp when remaining hits 0', () => {
    const onTimeUp = vi.fn()
    render(<Timer duration={1} onTimeUp={onTimeUp} difficulty="EASY" />)
    act(() => vi.advanceTimersByTime(1000))
    expect(onTimeUp).toHaveBeenCalledTimes(1)
  })

  it('resets when duration changes', () => {
    const { rerender } = render(
      <Timer duration={20} onTimeUp={() => {}} difficulty="EASY" />
    )
    act(() => vi.advanceTimersByTime(5000))
    expect(screen.getByText('00:15')).toBeInTheDocument()
    rerender(
      <Timer duration={60} onTimeUp={() => {}} difficulty="MEDIUM" />
    )
    expect(screen.getByText('01:00')).toBeInTheDocument()
  })
})
