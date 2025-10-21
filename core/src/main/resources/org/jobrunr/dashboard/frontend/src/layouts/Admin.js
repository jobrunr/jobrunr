import {createTheme, styled, ThemeProvider} from '@mui/material/styles';
import {Navigate, Route, Routes} from 'react-router';
import TopAppBar from "./TopAppBar";
import Overview from "../components/overview/overview";
import Servers from "../components/servers/servers";
import RecurringJobs from "../components/recurring-jobs/recurring-jobs";
import WithSidebar from "../components/utils/with-sidebar";
import JobView from "../components/jobs/job-view";
import JobsView from "../components/jobs/jobs-view";
import Sidebar from "../components/jobs/sidebar";
import GithubStarPopup from "../components/utils/github-star-popup";
import {DEFAULT_JOBRUNR_INFO, JobRunrInfoContext} from "../contexts/JobRunrInfoContext";
import {useEffect, useState} from "react";
import {setServers} from "../hooks/useServers";
import LoadingIndicator from "../components/LoadingIndicator.js";

const Main = styled("main")(({theme}) => ({
    padding: theme.spacing(3),
    marginTop: 56
}));

const theme = createTheme({
    palette: {
        primary: {
            main: '#000'
        },
        secondary: {
            main: '#f50057'
        }
    },
    components: {
        MuiLink: {
            styleOverrides: {
                root: {
                    color: "#26a8ed"
                }
            }
        }
    }
});

const App = () => {
    const JobViewWithSideBar = WithSidebar(Sidebar, JobView);
    const JobsViewWithSidebar = WithSidebar(Sidebar, JobsView);

    return (
        <div>
            <GithubStarPopup/>
            <TopAppBar/>
            <Main>
                <Routes>
                    <Route path="overview" element={<Overview/>}/>
                    <Route path="jobs/:jobId" element={<JobViewWithSideBar/>}/>
                    <Route path="jobs" element={<JobsViewWithSidebar/>}/>
                    <Route path="recurring-jobs" element={<RecurringJobs/>}/>
                    <Route path="servers" element={<Servers/>}/>
                    <Route path="*" element={<Navigate to="/dashboard/overview" replace/>}/>
                </Routes>
            </Main>
        </div>
    );
}

const AdminUI = function () {
    const [isLoading, setIsLoading] = useState(true);
    const [jobRunrInfo, setJobRunrInfo] = useState(DEFAULT_JOBRUNR_INFO);

    useEffect(() => {
        Promise.all([
            fetch("/api/servers").then(res => res.json()),
            fetch("/api/version").then(res => res.json()),
        ]).then(([servers, jobRunrInfo]) => {
            setServers(servers);
            setJobRunrInfo(jobRunrInfo);
        }).catch(error => console.log(error))
            .finally(() => setIsLoading(false));
    }, []);

    return (
        <ThemeProvider theme={theme}>
            {isLoading ?
                <LoadingIndicator/>
                : <JobRunrInfoContext value={jobRunrInfo}>
                    <App/>
                </JobRunrInfoContext>
            }
        </ThemeProvider>
    );
};

export default AdminUI;