import React from 'react'
import { screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { render } from '@testing-library/react'
import WelcomeBackModal from './WelcomeBackModal'

describe('WelcomeBackModal', () => {
  it('does not render modal content when closed', () => {
    render(
      <WelcomeBackModal isOpen={false} onResume={() => {}} onStartNew={() => {}} />
    )
    expect(screen.queryByText('Welcome Back!')).not.toBeInTheDocument()
  })

  it('renders title and buttons when open', () => {
    render(
      <WelcomeBackModal isOpen={true} onResume={() => {}} onStartNew={() => {}} />
    )
    expect(screen.getByText('Welcome Back!')).toBeInTheDocument()
    expect(screen.getByText('Start New')).toBeInTheDocument()
    expect(screen.getByText('Resume Interview')).toBeInTheDocument()
  })

  it('calls onStartNew when Start New is clicked', () => {
    const onStartNew = vi.fn()
    render(
      <WelcomeBackModal
        isOpen={true}
        onResume={() => {}}
        onStartNew={onStartNew}
      />
    )
    fireEvent.click(screen.getByText('Start New'))
    expect(onStartNew).toHaveBeenCalledTimes(1)
  })

  it('calls onResume when Resume Interview is clicked', () => {
    const onResume = vi.fn()
    render(
      <WelcomeBackModal
        isOpen={true}
        onResume={onResume}
        onStartNew={() => {}}
      />
    )
    fireEvent.click(screen.getByText('Resume Interview'))
    expect(onResume).toHaveBeenCalledTimes(1)
  })
})
