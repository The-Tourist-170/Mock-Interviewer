import React, { useState } from "react";
import PageWrapper from "../components/PageWrapper";
import { UsersRoundIcon, LayoutDashboard } from "lucide-react";
import NewInterview from "./NewInterview";
import Dashboard from "../components/Dashboard";

const Home = () => {
    const [defaultView, setDefaultView] = useState(0);

    return (
        <PageWrapper>
            <div className="mb-8">
                <div className="inline-flex gap-1 bg-surface p-1 rounded-lg">
                    <button
                        onClick={() => setDefaultView(0)}
                        className={`px-4 py-2 flex items-center gap-2 rounded-md transition-colors ${
                            defaultView === 0
                                ? "bg-white shadow-sm text-text-main font-medium"
                                : "text-text-muted hover:text-text-main"
                        }`}
                    >
                        <UsersRoundIcon size={18} className={defaultView === 0 ? "text-primary" : "text-text-muted"} />
                        Interviewer
                    </button>
                    <button
                        onClick={() => setDefaultView(1)}
                        className={`px-4 py-2 flex items-center gap-2 rounded-md transition-colors ${
                            defaultView === 1
                                ? "bg-white shadow-sm text-text-main font-medium"
                                : "text-text-muted hover:text-text-main"
                        }`}
                    >
                        <LayoutDashboard size={18} className={defaultView === 1 ? "text-primary" : "text-text-muted"} />
                        Dashboard
                    </button>
                </div>
                <div className="mt-6">
                    {defaultView === 0 ? <NewInterview /> : <Dashboard />}
                </div>
            </div>
        </PageWrapper>
    );
};

export default Home;
