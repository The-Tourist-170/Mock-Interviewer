const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

export const extractResumeInfo = async (resumeFile) => {
  const formData = new FormData();
  formData.append('resume', resumeFile);

  const response = await fetch(`${API_BASE_URL}/resumes/extract-info`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    throw new Error('Failed to extract resume information.');
  }
  return response.json();
};

export const startInterviewAPI = async (details) => {
  const { name, email, phone, resume } = details;
  const formData = new FormData();
  formData.append('name', name);
  formData.append('email', email);
  formData.append('phone', phone);
  formData.append('resume', resume);

  const response = await fetch(`${API_BASE_URL}/interviews/start`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    throw new Error('Failed to start the interview.');
  }
  return response.json();
};

export const submitAnswerAPI = async (sessionId, answer) => {
  const response = await fetch(`${API_BASE_URL}/interviews/${sessionId}/answer`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ answer }),
  });

  if (!response.ok) {
    throw new Error('Failed to submit answer.');
  }
  return response.json();
};

export const fetchCandidatesAPI = async () => {
  const response = await fetch(`${API_BASE_URL}/interviews/candidates`);
  if (!response.ok) {
    throw new Error('Failed to fetch candidates.');
  }
  return response.json();
};

export const fetchCandidateDetailAPI = async (candidateId) => {
  const response = await fetch(`${API_BASE_URL}/interviews/candidates/${candidateId}`);
  if (!response.ok) {
    throw new Error('Failed to fetch candidate details.');
  }
  return response.json();
};