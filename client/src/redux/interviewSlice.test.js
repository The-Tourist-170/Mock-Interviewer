import { describe, it, expect } from 'vitest';
import interviewReducer, {
  startInterview,
  interviewStarted,
  interviewFailed,
  submitAnswer,
  syncState,
  nextQuestionReceived,
  setResumeFile,
  resetInterview,
} from './interviewSlice';

const initialState = {
  sessionId: null,
  questions: [],
  answers: [],
  status: 'IDLE',
  difficulty: null,
  timer: null,
  isLoading: false,
  error: null,
  resumeFile: null,
};

describe('interviewSlice', () => {
  it('returns the initial state by default', () => {
    expect(interviewReducer(undefined, { type: 'unknown' })).toEqual(initialState);
  });

  describe('startInterview', () => {
    it('sets isLoading=true and clears error', () => {
      const prev = { ...initialState, error: 'boom', isLoading: false };
      const next = interviewReducer(prev, startInterview());
      expect(next.isLoading).toBe(true);
      expect(next.error).toBeNull();
    });
  });

  describe('interviewStarted', () => {
    it('sets sessionId, status=IN_PROGRESS, first question, difficulty, timer, clears loading+error', () => {
      const payload = {
        sessionId: 'sess-123',
        currentQuestionText: 'Tell me about yourself.',
        difficulty: 'EASY',
        timer: 20,
      };
      const next = interviewReducer(initialState, interviewStarted(payload));
      expect(next.sessionId).toBe('sess-123');
      expect(next.status).toBe('IN_PROGRESS');
      expect(next.questions).toEqual(['Tell me about yourself.']);
      expect(next.answers).toEqual([]);
      expect(next.difficulty).toBe('EASY');
      expect(next.timer).toBe(20);
      expect(next.isLoading).toBe(false);
      expect(next.error).toBeNull();
    });
  });

  describe('interviewFailed', () => {
    it('sets error and clears isLoading', () => {
      const prev = { ...initialState, isLoading: true };
      const next = interviewReducer(prev, interviewFailed('network error'));
      expect(next.isLoading).toBe(false);
      expect(next.error).toBe('network error');
    });
  });

  describe('submitAnswer', () => {
    it('pushes the answer and sets isLoading=true', () => {
      const prev = { ...initialState, questions: ['q1'], answers: [] };
      const next = interviewReducer(prev, submitAnswer({ answer: 'a1' }));
      expect(next.answers).toEqual(['a1']);
      expect(next.isLoading).toBe(true);
    });
  });

  describe('nextQuestionReceived', () => {
    it('IN_PROGRESS: pushes the next question and sets difficulty/timer/status', () => {
      const prev = {
        ...initialState,
        questions: ['q1'],
        answers: ['a1'],
        status: 'IN_PROGRESS',
        difficulty: 'EASY',
        timer: 20,
        isLoading: true,
      };
      const action = nextQuestionReceived({
        currentQuestionText: 'q2',
        difficulty: 'MEDIUM',
        timer: 60,
        status: 'IN_PROGRESS',
      });
      const next = interviewReducer(prev, action);
      expect(next.questions).toEqual(['q1', 'q2']);
      expect(next.difficulty).toBe('MEDIUM');
      expect(next.timer).toBe(60);
      expect(next.status).toBe('IN_PROGRESS');
      expect(next.isLoading).toBe(false);
    });

    it('COMPLETED: does NOT push "Interview Complete!" as a question (SR-M7 fix)', () => {
      const prev = {
        ...initialState,
        questions: ['q1', 'q2', 'q3', 'q4', 'q5', 'q6'],
        answers: ['a1', 'a2', 'a3', 'a4', 'a5', 'a6'],
        status: 'IN_PROGRESS',
        difficulty: 'HARD',
        timer: 120,
        isLoading: true,
      };
      const action = nextQuestionReceived({
        currentQuestionText: 'Interview Complete!',
        difficulty: null,
        timer: 0,
        status: 'COMPLETED',
      });
      const next = interviewReducer(prev, action);
      expect(next.questions).toEqual(['q1', 'q2', 'q3', 'q4', 'q5', 'q6']);
      expect(next.status).toBe('COMPLETED');
      expect(next.difficulty).toBeNull();
      expect(next.timer).toBe(0);
      expect(next.isLoading).toBe(false);
    });

    it('COMPLETED with null currentQuestionText does not push', () => {
      const prev = {
        ...initialState,
        questions: ['q1'],
        answers: ['a1'],
        status: 'IN_PROGRESS',
        isLoading: true,
      };
      const action = nextQuestionReceived({
        currentQuestionText: null,
        difficulty: null,
        timer: 0,
        status: 'COMPLETED',
      });
      const next = interviewReducer(prev, action);
      expect(next.questions).toEqual(['q1']);
      expect(next.status).toBe('COMPLETED');
    });
  });

  describe('syncState', () => {
    it('merges payload into state', () => {
      const prev = { ...initialState, status: 'IDLE' };
      const next = interviewReducer(prev, syncState({ status: 'IN_PROGRESS', sessionId: 's1' }));
      expect(next.status).toBe('IN_PROGRESS');
      expect(next.sessionId).toBe('s1');
      expect(next.questions).toEqual([]);
    });
  });

  describe('setResumeFile', () => {
    it('sets the resumeFile payload', () => {
      const file = { name: 'resume.pdf' };
      const next = interviewReducer(initialState, setResumeFile(file));
      expect(next.resumeFile).toEqual(file);
    });
  });

  describe('resetInterview', () => {
    it('resets to initialState', () => {
      const prev = {
        sessionId: 's1',
        questions: ['q1'],
        answers: ['a1'],
        status: 'COMPLETED',
        difficulty: 'HARD',
        timer: 120,
        isLoading: false,
        error: null,
        resumeFile: null,
      };
      const next = interviewReducer(prev, resetInterview());
      expect(next).toEqual(initialState);
    });
  });
});
