import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useParams, useNavigate } from 'react-router-dom';
import { submitAnswerAPI } from '../api/apiService';
import { submitAnswer, nextQuestionReceived, interviewFailed, resetInterview } from '../redux/interviewSlice';
import { AnimatePresence } from 'framer-motion';
import { Send, User, Loader2, Home } from 'lucide-react';
import { Toaster, toast } from 'react-hot-toast';
import Timer from '../components/Timer';

const IntervieweeChat = () => {
  const { sessionId } = useParams();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [currentAnswer, setCurrentAnswer] = useState('');
  const currentAnswerRef = useRef(currentAnswer);
  const isLoadingRef = useRef(false);

  const { questions, answers, difficulty, status, isLoading, timer } = useSelector((state) => state.interview);
  const chatEndRef = useRef(null);

  const currentQuestion = questions.length > answers.length ? questions[questions.length - 1] : null;

  useEffect(() => {
    currentAnswerRef.current = currentAnswer;
  }, [currentAnswer]);

  useEffect(() => {
    isLoadingRef.current = isLoading;
  }, [isLoading]);

  useEffect(() => {
    if (!sessionId || status === 'IDLE') {
      navigate('/interview/new');
    }
  }, [sessionId, status, navigate]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [questions, answers]);

  const handleAnswerSubmit = useCallback(async (e, { isAutoSubmit = false } = {}) => {
    if (e) e.preventDefault();

    let answerToSubmit = currentAnswerRef.current.trim();
    const displayAnswer = answerToSubmit || '';

    const isLastQuestion = questions.length === answers.length + 1;

    if ((!isAutoSubmit && !answerToSubmit) || isLoadingRef.current || (status === 'COMPLETED' && !isLastQuestion) ) {
      return;
    }

    const apiAnswer = answerToSubmit || "(No answer provided)";

    dispatch(submitAnswer({ answer: displayAnswer }));
    setCurrentAnswer('');

    try {
      const data = await submitAnswerAPI(sessionId, apiAnswer);
      dispatch(nextQuestionReceived(data));
    } catch (err) {
      dispatch(interviewFailed(err.message));
      toast.error(err.message);
    }
  }, [dispatch, sessionId, status, questions.length, answers.length]);

  const handleTimeUp = useCallback(() => {
    handleAnswerSubmit(null, { isAutoSubmit: true });
  }, [handleAnswerSubmit]);

  const handleGoToDashboard = () => {
    dispatch(resetInterview());
    navigate('/');
  };

  const messages = questions.flatMap((q, i) =>
    answers[i] ? [{ type: 'bot', text: q }, { type: 'user', text: answers[i] }] : [{ type: 'bot', text: q }]
  );

  return (
    <div>
      <Toaster position="top-center" />
      <div className="m-8 mx-auto max-w-3xl h-[calc(100vh-9rem)] flex flex-col">
        <div className="bg-white border border-border rounded-lg shadow-sm flex flex-col p-4 flex-1 overflow-hidden">
          <div className="flex-1 overflow-y-auto pr-2 space-y-4">
            {messages.map((msg, index) => (
              <div
                key={index}
                className={`flex items-start gap-3 ${msg.type === 'user' ? 'justify-end' : ''}`}
              >
                {msg.type === 'bot' && <div className="flex-shrink-0 w-8 h-8 rounded-full bg-surface-2 text-text-muted flex items-center justify-center text-xs font-medium">AI</div>}
                <div className={`max-w-md p-3 rounded-lg ${msg.type === 'bot' ? 'bg-surface text-text-main rounded-tl-none' : 'bg-primary text-white rounded-tr-none'}`}>
                  <p>{msg.text}</p>
                </div>
                {msg.type === 'user' && <div className="flex-shrink-0 w-8 h-8 rounded-full bg-surface-2 text-text-muted flex items-center justify-center"><User size={16} /></div>}
              </div>
            ))}
            <div ref={chatEndRef} />
          </div>
          <AnimatePresence>
            {status === 'COMPLETED' ? (
              <div className="text-center mt-4">
                <button
                  onClick={handleGoToDashboard}
                  className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-primary hover:bg-primary-hover text-white font-medium rounded-lg transition-colors"
                >
                  <Home size={18} /> Home
                </button>
              </div>
            ) : (
              <div className="mt-4 pt-4 border-t border-border">
                <form onSubmit={handleAnswerSubmit} className="flex items-center gap-3">
                  <input
                    type="text"
                    value={currentAnswer}
                    onChange={(e) => setCurrentAnswer(e.target.value)}
                    placeholder="Type your answer..."
                    disabled={isLoading}
                    className="flex-1 bg-white border border-border rounded-lg px-4 py-2.5 focus:ring-2 focus:ring-primary focus:border-primary focus:outline-none disabled:opacity-50"
                  />
                  <button
                    type="submit"
                    disabled={isLoading || !currentAnswer.trim()}
                    className="w-10 h-10 bg-primary hover:bg-primary-hover rounded-lg flex items-center justify-center text-white disabled:bg-primary/50 transition-colors"
                  >
                    {isLoading ? <Loader2 className="animate-spin" size={18} /> : <Send size={18} />}
                  </button>
                </form>
              </div>
            )}
          </AnimatePresence>
        </div>
        {currentQuestion && status !== 'COMPLETED' && (
          <div className="mt-6 flex justify-center">
            <div className="flex flex-col items-center gap-3 bg-white border border-border rounded-lg p-4 shadow-sm">
              <Timer key={currentQuestion.questionId} duration={timer} onTimeUp={handleTimeUp} difficulty={difficulty} />
              <span className="text-xs font-medium text-primary bg-blue-50 px-2 py-0.5 rounded-md">{difficulty}</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default IntervieweeChat;
