import { createTheme, styled, ThemeProvider, StyledEngineProvider } from '@mui/material/styles';
import { Navigate, Route, Routes } from 'react-router-dom';
import TopAppBar from "./TopAppBar";
import Overview from "../components/overview/overview";
import Servers from "../components/servers/servers";
import RecurringJobs from "../components/recurring-jobs/recurring-jobs";
import WithSidebar from "../components/utils/with-sidebar";
import JobView from "../components/jobs/job-view";
import JobsView from "../components/jobs/jobs-view";
import Sidebar from "../components/jobs/sidebar";
import GithubStarPopup from "../components/utils/github-star-popup";


const Main = styled("main")(({ theme }) => ({
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
                    <Route path="overview" element={<Overview />}/>
                    <Route path="jobs/:jobId" element={<JobViewWithSideBar />}/>
                    <Route path="jobs" element={<JobsViewWithSidebar />}/>
                    <Route path="recurring-jobs" element={<RecurringJobs />}/>
                    <Route path="servers" element={<Servers />}/>
                    <Route path="*" element={<Navigate to="overview" replace/>} />
                </Routes>
            </Main>
        </div>
    );
}

const AdminUI = function () {
    return (
        <StyledEngineProvider injectFirst>
            <ThemeProvider theme={theme}>
                <App />
            </ThemeProvider>
        </StyledEngineProvider>
    );
};

export default AdminUI;