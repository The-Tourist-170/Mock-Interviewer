import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  sessionId: null,
  questions: [],
  answers: [],
  status: 'IDLE',
  difficulty:null,
  timer: null,
  isLoading: false,
  error: null,
  resumeFile: null,
};

const interviewSlice = createSlice({
  name: 'interview',
  initialState,
  reducers: {
    startInterview: (state) => {
      state.isLoading = true;
      state.error = null;
    },
    interviewStarted: (state, action) => {
      state.sessionId = action.payload.sessionId;
      state.status = 'IN_PROGRESS';
      state.questions = [action.payload.currentQuestionText];
      state.answers = [];
      state.difficulty = action.payload.difficulty;
      state.timer = action.payload.timer;
      state.isLoading = false;
      state.error = null;
    },
    interviewFailed: (state, action) => {
      state.isLoading = false;
      state.error = action.payload;
    },
    syncState: (state, action) => {
      return { ...state, ...action.payload };
    },
    submitAnswer: (state, action) => {
      state.isLoading = true;
      state.answers.push(action.payload.answer);
    },
    nextQuestionReceived: (state, action) => {
      state.isLoading = false;
      if (action.payload.currentQuestionText && action.payload.status !== 'COMPLETED') {
        state.questions.push(action.payload.currentQuestionText);
      }
      state.difficulty = action.payload.difficulty;
      state.timer = action.payload.timer;
      state.status = action.payload.status;
    },
    setResumeFile: (state, action) => {
      state.resumeFile = action.payload;
    },
    resetInterview: () => initialState,
  },
});

export const {
  startInterview,
  interviewStarted,
  interviewFailed,
  submitAnswer,
  syncState,
  nextQuestionReceived,
  setResumeFile,
  resetInterview,
} = interviewSlice.actions;

export default interviewSlice.reducer;