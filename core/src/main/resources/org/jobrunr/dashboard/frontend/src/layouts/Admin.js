import {createTheme, styled, StyledEngineProvider, ThemeProvider} from '@mui/material/styles';
import {Navigate, Route, Routes} from 'react-router-dom';
import TopAppBar from "./TopAppBar";
import Overview from "../components/overview/overview";
import Servers from "../components/servers/servers";
import RecurringJobs from "../components/recurring-jobs/recurring-jobs";
import WithSidebar from "../components/utils/with-sidebar";
import JobView from "../components/jobs/job-view";
import JobsView from "../components/jobs/jobs-view";
import Sidebar from "../components/jobs/sidebar";
import GithubStarPopup from "../components/utils/github-star-popup";
import {DEFAULT_JOBRUNR_INFO, JobRunrInfoContext} from "../JobRunrInfoContext";
import {useCallback, useEffect, useMemo, useState} from "react";
import {ProblemsContext} from "../ProblemsContext";
import {getNewVersionProblem, LATEST_DISMISSED_VERSION_STORAGE_KEY} from "../components/overview/problems/new-jobrunr-version-available";
import {getApiNotificationProblem} from "../components/overview/problems/jobrunr-api-notification";


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
                    <Route path="*" element={<Navigate to="overview" replace/>}/>
                </Routes>
            </Main>
        </div>
    );
}

const AdminUI = function () {
    const [jobRunrInfo, setJobRunrInfo] = useState(DEFAULT_JOBRUNR_INFO);
    const [problems, setProblems] = useState([]);
    const [isLoadingProblems, setIsLoadingProblems] = useState(false);
    const [latestVersion, setLatestVersion] = useState(false);
    const [apiNotification, setApiNotification] = useState();

    const resetLatestVersion = useCallback(() => {
        setLatestVersion(undefined);
    }, []);

    const reloadProblems = useCallback(() => {
        setIsLoadingProblems(true);
        fetch(`/api/problems`).then(res => res.json())
            .then(problems => setProblems(problems))
            .catch(e => console.error("Failed to reload problems", e))
            .finally(() => setIsLoadingProblems(false));
    }, []);

    useEffect(() => {
        Promise.all([
            fetch("/api/version").then(res => res.json()),
            fetch("/api/problems").then(res => res.json()),
            fetch("https://api.jobrunr.io/api/version/jobrunr/latest").then(res => res.json()).catch(() => undefined /* ignored */),
            fetch("https://api.jobrunr.io/api/notifications/jobrunr").then(res => res.json()).catch(() => undefined /* ignored */),
        ]).then(([jobRunrInfo, problems, latestVersion, apiNotification]) => {
            setJobRunrInfo(jobRunrInfo);
            setProblems(problems);
            setLatestVersion(latestVersion["latestVersion"]);
            setApiNotification(apiNotification);
        }).catch(error => console.log(error));
    }, []);

    const problemsContext = useMemo(() => {
            const p = [...problems];
            if (latestVersion && localStorage.getItem(LATEST_DISMISSED_VERSION_STORAGE_KEY) !== latestVersion) {
                p.push({
                    ...getNewVersionProblem(jobRunrInfo.version, latestVersion),
                    reset: resetLatestVersion
                });
            }
            if (apiNotification) p.push(getApiNotificationProblem(apiNotification));

            return {
                problems: p,
                isLoading: isLoadingProblems,
                reload: reloadProblems
            }
        },
        [problems, reloadProblems, isLoadingProblems, latestVersion, resetLatestVersion, jobRunrInfo.version, apiNotification]
    );

    return (
        <StyledEngineProvider injectFirst>
            <ThemeProvider theme={theme}>
                <JobRunrInfoContext.Provider value={jobRunrInfo}>
                    <ProblemsContext.Provider value={problemsContext}>
                        <App/>
                    </ProblemsContext.Provider>
                </JobRunrInfoContext.Provider>
            </ThemeProvider>
        </StyledEngineProvider>
    );
};

export default AdminUI;