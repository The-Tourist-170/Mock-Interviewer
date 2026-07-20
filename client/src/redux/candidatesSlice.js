import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  list: [],
  selectedCandidate: null,
  isLoading: false,
  error: null,
};

const candidatesSlice = createSlice({
  name: 'candidates',
  initialState,
  reducers: {
    fetchCandidatesStart: (state) => {
      state.isLoading = true;
    },
    fetchCandidatesSuccess: (state, action) => {
      state.isLoading = false;
      state.list = action.payload;
    },
    fetchCandidateDetailSuccess: (state, action) => {
      state.isLoading = false;
      state.selectedCandidate = action.payload;
    },
    fetchCandidatesFailure: (state, action) => {
      state.isLoading = false;
      state.error = action.payload;
    },
    clearSelectedCandidate: (state) => {
        state.selectedCandidate = null;
    }
  },
});

export const {
  fetchCandidatesStart,
  fetchCandidatesSuccess,
  fetchCandidateDetailSuccess,
  fetchCandidatesFailure,
  clearSelectedCandidate
} = candidatesSlice.actions;

export default candidatesSlice.reducer;