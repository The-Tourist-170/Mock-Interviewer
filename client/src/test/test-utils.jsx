import React from 'react'
import { render } from '@testing-library/react'
import { configureStore } from '@reduxjs/toolkit'
import { Provider } from 'react-redux'
import { MemoryRouter } from 'react-router-dom'
import interviewReducer from '../redux/interviewSlice'
import candidatesReducer from '../redux/candidatesSlice'

export function renderWithProviders(
  ui,
  { preloadedState = {}, route = '/', ...renderOptions } = {}
) {
  const store = configureStore({
    reducer: { interview: interviewReducer, candidates: candidatesReducer },
    preloadedState,
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({ serializableCheck: false }),
  })

  function Wrapper({ children }) {
    return (
      <Provider store={store}>
        <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
      </Provider>
    )
  }

  return { store, ...render(ui, { wrapper: Wrapper, ...renderOptions }) }
}
