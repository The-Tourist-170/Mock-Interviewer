import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { fetchCandidatesAPI } from '../api/apiService';
import { fetchCandidatesStart, fetchCandidatesSuccess, fetchCandidatesFailure } from '../redux/candidatesSlice';
import PageWrapper from './PageWrapper';
import Spinner from './Spinner';
import { LayoutDashboardIcon } from 'lucide-react';

const Dashboard = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { list: candidates, isLoading, error } = useSelector((state) => state.candidates);

  useEffect(() => {
    const loadCandidates = async () => {
      dispatch(fetchCandidatesStart());
      try {
        const data = await fetchCandidatesAPI();
        dispatch(fetchCandidatesSuccess(data));
      } catch (err) {
        dispatch(fetchCandidatesFailure(err.message));
      }
    };
    loadCandidates();
  }, [dispatch]);

  const ScoreBadge = ({ score }) => {
    let bgColor = 'bg-red-100 text-red-800';
    if (score >= 7) bgColor = 'bg-green-100 text-green-800';
    else if (score >= 4) bgColor = 'bg-yellow-100 text-yellow-800';

    return (
      <span className={`px-3 py-1 text-sm font-semibold rounded-full ${bgColor}`}>
        {score} / 10
      </span>
    );
  };

  return (
    <PageWrapper>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-2xl font-semibold text-text-main flex items-center gap-3">
          <LayoutDashboardIcon className="w-7 h-7 text-primary" />
          Dashboard
        </h1>
      </div>

      <div className="bg-white border border-border rounded-lg shadow-sm">
        <div className="overflow-x-auto">
          {isLoading ? (
            <div className="flex justify-center items-center h-64">
              <Spinner size="lg" />
            </div>
          ) : error ? (
            <div className="text-center text-danger p-8">{error}</div>
          ) : (
            <table className="w-full text-left divide-y divide-border">
              <thead className="bg-surface">
                <tr>
                  <th className="p-4 text-xs font-medium uppercase tracking-wide text-text-muted">Candidate</th>
                  <th className="p-4 text-xs font-medium uppercase tracking-wide text-text-muted hidden md:table-cell">Summary</th>
                  <th className="p-4 text-xs font-medium uppercase tracking-wide text-text-muted text-right">Score</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {candidates.length > 0 ? candidates.map((candidate) => (
                  <tr
                    key={candidate.id}
                    className="hover:bg-surface cursor-pointer transition-colors"
                    onClick={() => navigate(`/candidate/${candidate.id}`)}
                  >
                    <td className="p-4 text-text-main font-medium">{candidate.name}</td>
                    <td className="p-4 text-text-muted max-w-lg truncate hidden md:table-cell">{candidate.summary}</td>
                    <td className="p-4 text-right">
                      <ScoreBadge score={candidate.score} />
                    </td>
                  </tr>
                )) : (
                  <tr>
                    <td colSpan="3" className="text-center p-8 text-text-muted">No candidates found. Start a new interview!</td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </PageWrapper>
  );
};

export default Dashboard;
