import { describe, it, expect } from 'vitest';
import candidatesReducer, {
  fetchCandidatesStart,
  fetchCandidatesSuccess,
  fetchCandidateDetailSuccess,
  fetchCandidatesFailure,
  clearSelectedCandidate,
} from './candidatesSlice';

const initialState = {
  list: [],
  selectedCandidate: null,
  isLoading: false,
  error: null,
};

describe('candidatesSlice', () => {
  it('returns the initial state by default', () => {
    expect(candidatesReducer(undefined, { type: 'unknown' })).toEqual(initialState);
  });

  describe('fetchCandidatesStart', () => {
    it('sets isLoading=true', () => {
      const next = candidatesReducer(initialState, fetchCandidatesStart());
      expect(next.isLoading).toBe(true);
    });
  });

  describe('fetchCandidatesSuccess', () => {
    it('sets list and clears isLoading', () => {
      const prev = { ...initialState, isLoading: true };
      const candidates = [{ id: '1', name: 'Alice', score: 8.5 }];
      const next = candidatesReducer(prev, fetchCandidatesSuccess(candidates));
      expect(next.list).toEqual(candidates);
      expect(next.isLoading).toBe(false);
    });
  });

  describe('fetchCandidateDetailSuccess', () => {
    it('sets selectedCandidate and clears isLoading', () => {
      const prev = { ...initialState, isLoading: true };
      const detail = { id: '1', name: 'Alice', score: 8.5, strengths: ['React'] };
      const next = candidatesReducer(prev, fetchCandidateDetailSuccess(detail));
      expect(next.selectedCandidate).toEqual(detail);
      expect(next.isLoading).toBe(false);
    });
  });

  describe('fetchCandidatesFailure', () => {
    it('sets error and clears isLoading', () => {
      const prev = { ...initialState, isLoading: true };
      const next = candidatesReducer(prev, fetchCandidatesFailure('server down'));
      expect(next.error).toBe('server down');
      expect(next.isLoading).toBe(false);
    });
  });

  describe('clearSelectedCandidate', () => {
    it('clears selectedCandidate', () => {
      const prev = { ...initialState, selectedCandidate: { id: '1', name: 'Alice' } };
      const next = candidatesReducer(prev, clearSelectedCandidate());
      expect(next.selectedCandidate).toBeNull();
    });
  });
});
