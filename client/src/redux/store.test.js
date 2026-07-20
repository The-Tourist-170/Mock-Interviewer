import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

describe('store', () => {
  let store;

  beforeEach(async () => {
    localStorage.clear();
    vi.resetModules();
    const mod = await import('./store');
    store = mod.store;
  });

  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('starts with initial state when localStorage is empty', () => {
    const state = store.getState();
    expect(state.interview).toEqual({
      sessionId: null,
      questions: [],
      answers: [],
      status: 'IDLE',
      difficulty: null,
      timer: null,
      isLoading: false,
      error: null,
      resumeFile: null,
    });
    expect(state.candidates).toEqual({
      list: [],
      selectedCandidate: null,
      isLoading: false,
      error: null,
    });
  });

  it('persists interview state to localStorage on dispatch', async () => {
    const { interviewStarted } = await import('./interviewSlice');
    store.dispatch(
      interviewStarted({
        sessionId: 'sess-abc',
        currentQuestionText: 'Why React?',
        difficulty: 'EASY',
        timer: 20,
      }),
    );
    const saved = JSON.parse(localStorage.getItem('interviewState'));
    expect(saved.sessionId).toBe('sess-abc');
    expect(saved.status).toBe('IN_PROGRESS');
    expect(saved.questions).toEqual(['Why React?']);
    expect(saved.timer).toBe(20);
  });

  it('excludes resumeFile from persisted state (CR-H3 fix)', async () => {
    const { setResumeFile, interviewStarted } = await import('./interviewSlice');
    store.dispatch(
      interviewStarted({
        sessionId: 'sess-xyz',
        currentQuestionText: 'q1',
        difficulty: 'MEDIUM',
        timer: 60,
      }),
    );
    store.dispatch(setResumeFile({ name: 'resume.pdf', size: 9999 }));

    const saved = JSON.parse(localStorage.getItem('interviewState'));
    expect(saved.resumeFile).toBeUndefined();
    expect(store.getState().interview.resumeFile).toEqual({ name: 'resume.pdf', size: 9999 });
  });

  it('loads preloaded state from localStorage on import', async () => {
    const persisted = {
      sessionId: 'sess-restored',
      questions: ['restored q'],
      answers: [],
      status: 'IN_PROGRESS',
      difficulty: 'HARD',
      timer: 120,
      isLoading: false,
      error: null,
    };
    localStorage.setItem('interviewState', JSON.stringify(persisted));

    vi.resetModules();
    const mod = await import('./store');
    const freshStore = mod.store;

    expect(freshStore.getState().interview.sessionId).toBe('sess-restored');
    expect(freshStore.getState().interview.status).toBe('IN_PROGRESS');
    expect(freshStore.getState().interview.timer).toBe(120);
  });

  it('returns undefined preloadedState when localStorage is corrupt', async () => {
    localStorage.setItem('interviewState', '{not valid json');
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    vi.resetModules();
    const mod = await import('./store');
    const freshStore = mod.store;
    expect(freshStore.getState().interview.sessionId).toBeNull();
    expect(consoleSpy).toHaveBeenCalled();
  });
});
