import React from 'react';
import { useNavigate } from 'react-router-dom';

const Logo = () => {
  const navigate = useNavigate();

  const handleHome = () => {
    navigate('/');
  };

  return (
    <div
      onClick={handleHome}
      style={{ cursor: 'pointer' }}
      className="flex items-center gap-2">
      <h1 className="text-xl font-semibold text-text-main">
        AI Interviewer
      </h1>
    </div>
  );
};

export default Logo;
