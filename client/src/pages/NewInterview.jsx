import React, { useState, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { extractResumeInfo, startInterviewAPI } from '../api/apiService';
import { setResumeFile, startInterview, interviewStarted, interviewFailed } from '../redux/interviewSlice';
import { Toaster, toast } from 'react-hot-toast';
import { motion, AnimatePresence } from 'framer-motion';
import { UploadCloud, User, Mail, Phone, Loader2, ArrowRight } from 'lucide-react';
import PageWrapper from '../components/PageWrapper';

const NewInterview = () => {
  const [step, setStep] = useState(1);
  const [isExtracting, setIsExtracting] = useState(false);
  const [formData, setFormData] = useState({ name: '', email: '', phone: '' });
  const fileInputRef = useRef(null);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { isLoading } = useSelector((state) => state.interview);
  const resumeFile = useSelector(state => state.interview.resumeFile);

  const handleFileChange = async (event) => {
    const file = event.target.files[0];
    if (file && file.type === 'application/pdf') {
      dispatch(setResumeFile(file));
      setIsExtracting(true);
      const toastId = toast.loading('Extracting info from resume...');
      try {
        const data = await extractResumeInfo(file);
        setFormData({
          name: data.name || '',
          email: data.email || '',
          phone: data.phone || '',
        });
        toast.dismiss(toastId);
        toast.success('Information extracted!');
        setStep(2);
      } catch (error) {
        toast.dismiss(toastId);
        toast.error(error.message);
      } finally {
        setIsExtracting(false);
      }
    } else if (file) {
      toast.error('Please upload a valid PDF file.');
    }
  };

  const handleInputChange = (event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!formData.name || !formData.email || !resumeFile) {
        toast.error('Name, Email, and a Resume file are required.');
        return;
    }
    dispatch(startInterview());
    try {
        const data = await startInterviewAPI({ ...formData, resume: resumeFile });
        dispatch(interviewStarted(data));
        navigate(`/interview/active/${data.sessionId}`);
    } catch (error) {
        dispatch(interviewFailed(error.message));
        toast.error(error.message);
    }
  };

  const cardVariants = {
    hidden: { opacity: 0, x: 100 },
    visible: { opacity: 1, x: 0, transition: { duration: 0.2, ease: "easeOut" } },
    exit: { opacity: 0, x: -100, transition: { duration: 0.2, ease: "easeIn" } },
  };

  return (
    <PageWrapper>
      <Toaster position="top-center" />
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-semibold text-center mb-8 text-text-main">Start New Interview</h1>
        <div className="min-h-[400px] flex items-center justify-center">
            <AnimatePresence mode="wait">
            {step === 1 && (
                <motion.div
                    key="step1"
                    variants={cardVariants}
                    initial="hidden"
                    animate="visible"
                    exit="exit"
                    className="w-full text-center"
                >
                    <input type="file" ref={fileInputRef} onChange={handleFileChange} accept=".pdf" className="hidden" />
                    <div
                        className="border-2 border-dashed border-border rounded-lg p-10 cursor-pointer bg-surface hover:border-primary transition-colors"
                        onClick={() => fileInputRef.current.click()}
                    >
                        <UploadCloud className="w-12 h-12 mx-auto text-text-muted" />
                        <p className="mt-4 font-medium text-text-main">
                            {isExtracting ? 'Analyzing Resume...' : 'Click to Upload Resume'}
                        </p>
                        <p className="text-sm text-text-muted">PDF only</p>
                        {isExtracting && <Loader2 className="w-6 h-6 mx-auto mt-4 animate-spin text-primary" />}
                    </div>
                </motion.div>
            )}

            {step === 2 && (
                <motion.div
                    key="step2"
                    variants={cardVariants}
                    initial="hidden"
                    animate="visible"
                    exit="exit"
                    className="w-full"
                >
                    <h2 className="text-lg font-semibold mb-6 text-center text-text-main">Confirm Candidate Details</h2>
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div className="relative">
                            <User className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted"/>
                            <input type="text" name="name" value={formData.name} onChange={handleInputChange} placeholder="Full Name" className="w-full bg-white border border-border rounded-lg pl-10 pr-4 py-2.5 focus:ring-2 focus:ring-primary focus:border-primary focus:outline-none"/>
                        </div>
                        <div className="relative">
                            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted"/>
                            <input type="email" name="email" value={formData.email} onChange={handleInputChange} placeholder="Email Address" className="w-full bg-white border border-border rounded-lg pl-10 pr-4 py-2.5 focus:ring-2 focus:ring-primary focus:border-primary focus:outline-none"/>
                        </div>
                        <div className="relative">
                            <Phone className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted"/>
                            <input type="tel" name="phone" value={formData.phone} onChange={handleInputChange} placeholder="Phone Number" className="w-full bg-white border border-border rounded-lg pl-10 pr-4 py-2.5 focus:ring-2 focus:ring-primary focus:border-primary focus:outline-none"/>
                        </div>
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full flex justify-center items-center gap-2 px-4 py-2.5 bg-primary hover:bg-primary-hover active:bg-primary text-white font-medium rounded-lg transition-colors disabled:bg-primary/50 disabled:cursor-not-allowed"
                        >
                            {isLoading ? <Loader2 className="animate-spin" /> : <>Start Interview <ArrowRight size={18} /></>}
                        </button>
                    </form>
                </motion.div>
            )}
            </AnimatePresence>
        </div>
      </div>
    </PageWrapper>
  );
};

export default NewInterview;
