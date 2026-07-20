import { configureStore } from '@reduxjs/toolkit';
import interviewReducer from './interviewSlice';
import candidatesReducer from './candidatesSlice';

const loadState = () => {
  try {
    const serializedState = localStorage.getItem('interviewState');
    if (serializedState === null) {
      return undefined;
    }
    return { interview: JSON.parse(serializedState) };
  } catch (err) {
    console.error("Could not load state", err);
    return undefined;
  }
};

const saveState = (state) => {
  try {
    const { resumeFile: _resumeFile, ...stateToSave } = state.interview;
    const serializedState = JSON.stringify(stateToSave);
    localStorage.setItem('interviewState', serializedState);
  } catch (err) {
    console.error("Could not save state", err);
  }
};

const preloadedState = loadState();

export const store = configureStore({
  reducer: {
    interview: interviewReducer,
    candidates: candidatesReducer,
  },
  preloadedState,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: false,
    }),
});

store.subscribe(() => {
  saveState(store.getState());
});