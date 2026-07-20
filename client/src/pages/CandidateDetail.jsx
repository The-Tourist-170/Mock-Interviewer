import React, { useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { fetchCandidateDetailAPI } from '../api/apiService';
import { fetchCandidatesStart, fetchCandidateDetailSuccess, fetchCandidatesFailure, clearSelectedCandidate } from '../redux/candidatesSlice';
import PageWrapper from '../components/PageWrapper';
import Spinner from '../components/Spinner';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { ArrowLeft, MessageSquare, ClipboardList, Star, TrendingUp, Code, BrainCircuit } from 'lucide-react';

const CandidateDetail = () => {
    const { candidateId } = useParams();
    const dispatch = useDispatch();
    const { selectedCandidate: candidate, isLoading, error } = useSelector((state) => state.candidates);

    useEffect(() => {
        dispatch(fetchCandidatesStart());
        const loadDetails = async () => {
            try {
                const data = await fetchCandidateDetailAPI(candidateId);
                dispatch(fetchCandidateDetailSuccess(data));
            } catch (err) {
                dispatch(fetchCandidatesFailure(err.message));
            }
        };
        loadDetails();

        return () => {
            dispatch(clearSelectedCandidate());
        }
    }, [candidateId, dispatch]);

    if (isLoading) {
        return <div className="flex justify-center items-center h-screen"><Spinner size="lg" /></div>;
    }

    if (error) {
        return <div className="text-center text-danger p-8">{error}</div>;
    }

    if (!candidate) return null;

    const experienceData = candidate.experienceBreakdown
        ? Object.entries(candidate.experienceBreakdown)
            .map(([name, value]) => ({
                name,
                value: Number(value) || 0
            }))
            .filter(item => item.value > 0)
        : [];

    const COLORS = ['#2563EB', '#16A34A', '#F59E0B', '#DC2626', '#0891B2', '#7C3AED'];

    const RADIAN = Math.PI / 180;
    const renderCustomizedLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }) => {
        const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
        const x = cx + radius * Math.cos(-midAngle * RADIAN);
        const y = cy + radius * Math.sin(-midAngle * RADIAN);

        return (
            <text x={x} y={y} fill="white" textAnchor={x > cx ? 'start' : 'end'} dominantBaseline="central">
                {`${(percent * 100).toFixed(0)}%`}
            </text>
        );
    };

    return (
        <PageWrapper>
            <div className="max-w-5xl mx-auto">
                <Link to="/" className="inline-flex items-center gap-2 text-primary hover:text-primary-hover mb-6 font-medium">
                    <ArrowLeft size={18} /> Back to Dashboard
                </Link>

                <div className="bg-white border border-border rounded-lg p-6 mb-6 shadow-sm">
                    <div className="flex flex-col md:flex-row justify-between items-start gap-4">
                        <div>
                            <h1 className="text-2xl font-semibold text-text-main">{candidate.name}</h1>
                            <div className="flex items-center gap-3 mt-2 text-text-muted text-sm">
                                <span>{candidate.email}</span>
                                <span>&bull;</span>
                                <span>{candidate.phone}</span>
                            </div>
                        </div>
                        <div className="bg-surface border border-border rounded-lg p-4 text-center">
                            <p className="text-xs font-medium text-text-muted uppercase tracking-wide">Final Score</p>
                            <p className="text-3xl font-semibold text-text-main mt-1">{candidate.score}</p>
                        </div>
                    </div>
                    <div className="mt-6">
                        <h3 className="text-base font-semibold text-text-main flex items-center gap-2"><Star size={18} /> AI Summary</h3>
                        <p className="mt-2 text-text-muted text-sm leading-relaxed">{candidate.summary}</p>
                    </div>
                </div>

                <div className="bg-white border border-border rounded-lg p-6 mb-6 shadow-sm">
                    <h2 className="text-lg font-semibold mb-4 flex items-center gap-2 text-text-main"><ClipboardList size={20} className="text-primary" /> Resume Analysis</h2>

                    <div className="space-y-6">
                        <div className="grid md:grid-cols-2 gap-6">
                            <div className="border border-border rounded-lg p-4">
                                <h3 className="font-medium text-text-main mb-3 flex items-center gap-2"><TrendingUp size={18} className="text-green-600" /> Strengths</h3>
                                <ul className="space-y-1.5 list-disc list-inside text-text-muted text-sm">
                                    {candidate.strengths?.map((item, index) => <li key={`strength-${index}`}>{item}</li>)}
                                </ul>
                            </div>

                            <div className="border border-border rounded-lg p-4">
                                <h3 className="font-medium text-text-main mb-3 flex items-center gap-2"><TrendingUp size={18} className="rotate-180 text-danger" /> Weaknesses</h3>
                                <ul className="space-y-1.5 list-disc list-inside text-text-muted text-sm">
                                    {candidate.weaknesses?.map((item, index) => <li key={`weakness-${index}`}>{item}</li>)}
                                </ul>
                            </div>
                        </div>

                        <div className="grid md:grid-cols-2 gap-6">
                            <div className="border border-border rounded-lg p-4">
                                <h3 className="font-medium text-text-main mb-3 flex items-center gap-2"><Code size={18} className="text-primary" /> Skill Ratings</h3>
                                <div className="space-y-3">
                                    {candidate.skillRatings && Object.entries(candidate.skillRatings).map(([skill, rating]) => (
                                        <div key={skill}>
                                            <div className="flex justify-between items-center mb-1 text-sm">
                                                <span className="text-text-main">{skill}</span>
                                                <span className="font-medium text-text-muted">{rating}/10</span>
                                            </div>
                                            <div className="w-full bg-surface-2 rounded-full h-2">
                                                <div className="bg-primary h-2 rounded-full" style={{ width: `${rating * 10}%` }}></div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="border border-border rounded-lg p-4">
                                <h3 className="font-medium text-text-main mb-3 flex items-center gap-2"><BrainCircuit size={18} className="text-primary" /> Experience Breakdown</h3>
                                <div style={{ width: '100%', height: 250 }}>
                                    <ResponsiveContainer>
                                        <PieChart>
                                            <Pie
                                                data={experienceData}
                                                cx="50%"
                                                cy="50%"
                                                labelLine={false}
                                                label={renderCustomizedLabel}
                                                outerRadius={80}
                                                fill="#2563EB"
                                                dataKey="value"
                                            >
                                                {experienceData.map((entry, index) => (
                                                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                                                ))}
                                            </Pie>
                                            <Tooltip
                                                contentStyle={{ background: '#FFFFFF', border: '1px solid #E5E7EB', borderRadius: '0.25rem', color: '#111827' }}
                                            />
                                            <Legend />
                                        </PieChart>
                                    </ResponsiveContainer>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="bg-white border border-border rounded-lg p-6 shadow-sm">
                    <h2 className="text-lg font-semibold mb-4 flex items-center gap-2 text-text-main">
                        <MessageSquare size={20} className="text-primary" /> Interview Transcript
                    </h2>
                    <div className="space-y-4">
                        {candidate.questions?.map((turn, index) => (
                            <div key={index} className="space-y-2">
                                <p className="font-medium text-text-main text-sm">Q: {turn.questionText}</p>
                                <p className="text-text-muted text-sm pl-4 border-l-2 border-primary bg-surface w-fit px-3 py-2 rounded-r-md">{turn.candidateAnswer}</p>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </PageWrapper>
    );
};

export default CandidateDetail;
